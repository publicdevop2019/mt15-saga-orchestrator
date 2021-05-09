package com.mt.saga.domain.model.task.conclude_order_task;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.saga.domain.DomainRegistry;
import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;
import com.mt.saga.domain.model.task.SubTaskStatus;
import com.mt.saga.domain.model.task.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@EnableScheduling
@Component
public class ConcludeOrderTaskScheduler {
    @Value("${task.expireAfter}")
    private Long taskExpireAfter;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;

    @Scheduled(fixedRateString = "${fixedRate.in.milliseconds.taskRollback}")
    public void rollbackTask() {
        log.debug("expired reserve tasks scanning started");
        Date from = Date.from(Instant.ofEpochMilli(Instant.now().toEpochMilli() - taskExpireAfter * 60 * 1000));
        List<ConcludeOrderTask> tasks = DomainRegistry.getConcludeOrderTaskRepository().findRollbackTasks(from);
        if (!tasks.isEmpty()) {
            log.info("expired & started task found {}", tasks.stream().map(ConcludeOrderTask::getId).collect(Collectors.toList()));
            tasks.stream().limit(5).forEach(task -> {
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    Optional<ConcludeOrderTask> byIdOptLock = DomainRegistry.getConcludeOrderTaskRepository().findByIdLocked(task.getId());
                                    if (byIdOptLock.isPresent()
                                            && byIdOptLock.get().getCreatedAt().compareTo(from) < 0
                                            && (byIdOptLock.get().getTaskStatus().equals(TaskStatus.STARTED) || byIdOptLock.get().getTaskStatus().equals(TaskStatus.FAILED))
                                    ) {
                                        log.info("rolling back task with id {}", task.getId());
                                        cancelConcludeTask(task);
                                        log.info("rollback task {} success", task.getId());
                                    } else {
                                        log.info("task present is {}", byIdOptLock.isPresent());
                                    }

                                }
                            });
                } catch (Exception e) {
                    log.error("rollback task {} failed", task.getId(), e);
                }
            });
        }
    }

    private void cancelConcludeTask(ConcludeOrderTask bizTx) {
        OrderOperationEvent command = CommonDomainRegistry.getCustomObjectSerializer().deserialize(bizTx.getCreateBizStateMachineCommand(), OrderOperationEvent.class);
        log.info("start of cancel task of {} with {}", bizTx.getTaskId(), bizTx.getCancelTaskId());

        // cancel actual storage change
        CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                DomainRegistry.getProductService().cancelUpdateProductStorage(DomainRegistry.getProductService().getConfirmOrderPatchCommands(command.getProductList()), bizTx.getCancelTaskId(), bizTx.getTaskId()), customExecutor
        );

        // cancel order update
        CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                DomainRegistry.getOrderService().cancelConcludeOrder(command, bizTx.getCancelTaskId(), command.getTxId()), customExecutor
        );

        try {
            decreaseOrderStorageFuture.get();
            bizTx.setDecreaseActualStorageSubTaskStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException e) {
            log.error("error during order storage cancel", e);
            bizTx.setCancelBlocked(true);
            //do nothing
        }

        try {
            updateOrderFuture.get();
            bizTx.setUpdateOrderSubTaskStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException ex) {
            bizTx.setCancelBlocked(true);
            log.error("error during order cancel", ex);
            //do nothing
        }
        if (
                bizTx.getDecreaseActualStorageSubTaskStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getUpdateOrderSubTaskStatus().equals(SubTaskStatus.CANCELLED)
        ) {
            bizTx.setTaskStatus(TaskStatus.CANCELLED);
        }
        DomainRegistry.getConcludeOrderTaskRepository().add(bizTx);
    }

}

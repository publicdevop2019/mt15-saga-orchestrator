package com.hw.aggregate.tx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.aggregate.sm.CartService;
import com.hw.aggregate.sm.OrderService;
import com.hw.aggregate.sm.PaymentService;
import com.hw.aggregate.sm.ProductService;
import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.tx.model.CreateOrderTask;
import com.hw.aggregate.tx.model.RecycleOrderTask;
import com.hw.aggregate.tx.model.SubTaskStatus;
import com.hw.aggregate.tx.model.TaskStatus;
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

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@EnableScheduling
@Component
public class RecycleOrderTaskScheduler {
    @Autowired
    private RecycleOrderTaskRepository taskRepository;
    @Value("${task.expireAfter}")
    private Long taskExpireAfter;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;
    @Autowired
    private AppTaskApplicationService taskService;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;

    @Scheduled(fixedRateString = "${fixedRate.in.milliseconds.taskRollback}")
    public void rollbackTask() {
        log.debug("expired recycle tasks scanning started");
        Date from = Date.from(Instant.ofEpochMilli(Instant.now().toEpochMilli() - taskExpireAfter * 60 * 1000));
        List<RecycleOrderTask> tasks = taskRepository.findExpiredStartedOrFailTxs(from);
        if (!tasks.isEmpty()) {
            log.info("expired & started task found {}", tasks.stream().map(RecycleOrderTask::getId).collect(Collectors.toList()));
            tasks.stream().limit(5).forEach(task -> {
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    Optional<RecycleOrderTask> byIdOptLock = taskRepository.findByIdOptLock(task.getId());
                                    if (byIdOptLock.isPresent()
                                            && byIdOptLock.get().getCreatedAt().compareTo(from) < 0
                                            && (byIdOptLock.get().getTaskStatus().equals(TaskStatus.STARTED) || byIdOptLock.get().getTaskStatus().equals(TaskStatus.FAILED) )
                                    ) {
                                        log.info("rolling back task with id {}", task.getId());
                                        cancelRecycleTask(task);
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

    private void cancelRecycleTask(RecycleOrderTask bizTx) {
        CreateBizStateMachineCommand command;
        try {
            command = om.readValue(bizTx.getCreateBizStateMachineCommand(), CreateBizStateMachineCommand.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("unable to parse");
        }
        List<RuntimeException> exs = new ArrayList<>();
        log.info("start of cancel task of {} with {}", bizTx.getTaskId(), bizTx.getCancelTaskId());

        // cancel order storage change
        CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                productService.cancelUpdateProductStorage(productService.getReserveOrderPatchCommands(command.getProductList()), bizTx.getCancelTaskId(), bizTx.getTaskId()), customExecutor
        );

        // cancel order update
        CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                orderService.cancelRecycleOrder(command, bizTx.getCancelTaskId(), command.getTxId()), customExecutor
        );

        try {
            decreaseOrderStorageFuture.get();
            bizTx.setIncreaseOrderStorageSubTaskStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException e) {
            log.error("error during order storage cancel",e);
            //do nothing
        }

        try {
            updateOrderFuture.get();
            bizTx.setUpdateOrderSubTaskStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException ex) {
            log.error("error during order cancel",ex);
            //do nothing
        }
        if (
                bizTx.getIncreaseOrderStorageSubTaskStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getUpdateOrderSubTaskStatus().equals(SubTaskStatus.CANCELLED)
        ) {
            bizTx.setTaskStatus(TaskStatus.CANCELLED);
        }

        taskRepository.save(bizTx);
    }

}

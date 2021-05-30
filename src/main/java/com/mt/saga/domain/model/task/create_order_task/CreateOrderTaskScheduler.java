package com.mt.saga.domain.model.task.create_order_task;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.domain_event.DomainEventPublisher;
import com.mt.saga.domain.DomainRegistry;
import com.mt.saga.domain.model.order_state_machine.event.UserPlaceOrderEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse.CancelPaymentQRLinkEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse.DeleteNewOrderEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse.IncreaseOrderStorageEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse.RestoreCartEvent;
import com.mt.saga.domain.model.order_state_machine.order.CartDetail;
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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@EnableScheduling
@Component
public class CreateOrderTaskScheduler {
    @Value("${task.expireAfter}")
    private Long taskExpireAfter;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;

    @Scheduled(fixedRateString = "${fixedRate.in.milliseconds.taskRollback}")
    public void rollbackTask() {
        log.debug("expired create order tasks scanning started");
        Date from = Date.from(Instant.ofEpochMilli(Instant.now().toEpochMilli() - taskExpireAfter * 60 * 1000));
        List<CreateOrderTask> tasks = DomainRegistry.getCreateOrderTaskRepository().findRollbackTasks(from);
        if (!tasks.isEmpty()) {
            log.info("expired & started task found {}", tasks.stream().map(CreateOrderTask::getId).collect(Collectors.toList()));
            tasks.stream().limit(5).forEach(task -> {
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    Optional<CreateOrderTask> byIdOptLock = DomainRegistry.getCreateOrderTaskRepository().getById(task.getId());
                                    if (byIdOptLock.isPresent()
                                            && byIdOptLock.get().getCreatedAt().compareTo(from) < 0
                                            && (byIdOptLock.get().getTaskStatus().equals(TaskStatus.STARTED) || byIdOptLock.get().getTaskStatus().equals(TaskStatus.FAILED))
                                    ) {
                                        log.info("rolling back task with id {}", task.getId());
                                        rollbackCreate(task);
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

    private void rollbackCreate(CreateOrderTask bizTx) {
        UserPlaceOrderEvent command = CommonDomainRegistry.getCustomObjectSerializer().deserialize(bizTx.getCreateBizStateMachineCommand(), UserPlaceOrderEvent.class);
        log.info("start of cancel task of {} with {}", bizTx.getForwardChangeId(), bizTx.getReverseChangeId());
        DomainEventPublisher.instance().publish(new CancelPaymentQRLinkEvent(bizTx.getOrderId(), bizTx.getReverseChangeId(), bizTx.getId(), bizTx.getForwardChangeId()));
        DomainEventPublisher.instance().publish(new IncreaseOrderStorageEvent(DomainRegistry.getProductService().getReserveOrderPatchCommands(command.getProductList()), bizTx.getReverseChangeId(), bizTx.getId()));
        Set<String> collect = command.getProductList().stream().map(CartDetail::getCartId).collect(Collectors.toSet());
        DomainEventPublisher.instance().publish(new RestoreCartEvent(command.getUserId(), collect, bizTx.getReverseChangeId(), bizTx.getId(), bizTx.getForwardChangeId()));
        DomainEventPublisher.instance().publish(new DeleteNewOrderEvent(command.getOrderId(), command.getUserId(), bizTx.getId(), bizTx.getReverseChangeId(), bizTx.getForwardChangeId()));
        if (
                bizTx.getGeneratePaymentLinkSubTask().getStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getDecreaseOrderStorageSubTaskStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getRemoveItemsFromCartSubTaskStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getCreateOrderSubTask().getStatus().equals(SubTaskStatus.CANCELLED)
        ) {
            bizTx.setTaskStatus(TaskStatus.CANCELLED);
        }
        DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(bizTx);
    }

}

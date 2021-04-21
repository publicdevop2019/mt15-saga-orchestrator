package com.hw.aggregate.tx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.aggregate.sm.CartService;
import com.hw.aggregate.sm.OrderService;
import com.hw.aggregate.sm.PaymentService;
import com.hw.aggregate.sm.ProductService;
import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.model.order.CartDetail;
import com.hw.aggregate.tx.model.CreateOrderTask;
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
public class CreateOrderTaskScheduler {
    @Autowired
    private CreateOrderTaskRepository taskRepository;
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
        log.debug("expired create order tasks scanning started");
        Date from = Date.from(Instant.ofEpochMilli(Instant.now().toEpochMilli() - taskExpireAfter * 60 * 1000));
        List<CreateOrderTask> tasks = taskRepository.findExpiredStartedOrFailNonBlockedTxs(from);
        if (!tasks.isEmpty()) {
            log.info("expired & started task found {}", tasks.stream().map(CreateOrderTask::getId).collect(Collectors.toList()));
            tasks.stream().limit(5).forEach(task -> {
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    Optional<CreateOrderTask> byIdOptLock = taskRepository.findByIdOptLock(task.getId());
                                    if (byIdOptLock.isPresent()
                                            && byIdOptLock.get().getCreatedAt().compareTo(from) < 0
                                            && (byIdOptLock.get().getTaskStatus().equals(TaskStatus.STARTED) || byIdOptLock.get().getTaskStatus().equals(TaskStatus.FAILED) )
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
        CreateBizStateMachineCommand command;
        try {
            command = om.readValue(bizTx.getCreateBizStateMachineCommand(), CreateBizStateMachineCommand.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("unable to parse");
        }
        List<RuntimeException> exs = new ArrayList<>();
        log.info("start of cancel task of {} with {}", bizTx.getTaskId(), bizTx.getCancelTaskId());

        // cancel payment QR link
        CompletableFuture<Void> paymentQRLinkFuture = CompletableFuture.runAsync(() ->
                {
                    paymentService.cancelPaymentLink(bizTx.getCancelTaskId(), bizTx.getTaskId());
                }, customExecutor
        );

        // cancel sku
        CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                productService.cancelUpdateProductStorage(productService.getReserveOrderPatchCommands(command.getProductList()), bizTx.getCancelTaskId(), bizTx.getTaskId()), customExecutor
        );

        // cancel clear cart
        CompletableFuture<Void> clearCartFuture = CompletableFuture.runAsync(() -> {
                    Set<String> collect = command.getProductList().stream().map(CartDetail::getCartId).collect(Collectors.toSet());
                    cartService.cancelClearCart(command.getUserId(), collect, bizTx.getCancelTaskId(), bizTx.getTaskId());
                }, customExecutor
        );
        // cancel create order
        CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                orderService.cancelCreateNewOrder(command, bizTx.getCancelTaskId(), command.getTxId()), customExecutor
        );

        try {
            paymentQRLinkFuture.get();
            bizTx.getGeneratePaymentLinkSubTask().setStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException e) {
            log.error("error during payment cancel",e);
            bizTx.setCancelBlocked(true);
            //do nothing
        }
        try {
            decreaseOrderStorageFuture.get();
            bizTx.setDecreaseOrderStorageSubTaskStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException e) {
            log.error("error during order storage cancel",e);
            bizTx.setCancelBlocked(true);
            //do nothing
        }
        try {
            clearCartFuture.get();
            bizTx.setRemoveItemsFromCartSubTaskStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException e) {
            log.error("error during cart cancel",e);
            bizTx.setCancelBlocked(true);
            //do nothing
        }

        try {
            updateOrderFuture.get();
            bizTx.getCreateOrderSubTask().setStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException ex) {
            log.error("error during order cancel",ex);
            bizTx.setCancelBlocked(true);
            //do nothing
        }
        if (
                bizTx.getGeneratePaymentLinkSubTask().getStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getDecreaseOrderStorageSubTaskStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getRemoveItemsFromCartSubTaskStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getCreateOrderSubTask().getStatus().equals(SubTaskStatus.CANCELLED)
        ) {
            bizTx.setTaskStatus(TaskStatus.CANCELLED);
        }

        taskRepository.save(bizTx);
    }

}

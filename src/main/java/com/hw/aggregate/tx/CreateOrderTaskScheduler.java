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
        List<CreateOrderTask> tasks = taskRepository.findExpiredStartedOrFailTxs(from);
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
                                            && (byIdOptLock.get().getTxStatus().equals(TaskStatus.STARTED) || byIdOptLock.get().getTxStatus().equals(TaskStatus.FAILED) )
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
        log.info("start of cancel task of {} with {}", bizTx.getTxId(), bizTx.getCancelTxId());

        // cancel payment QR link
        CompletableFuture<Void> paymentQRLinkFuture = CompletableFuture.runAsync(() ->
                {
                    paymentService.cancelPaymentLink(bizTx.getCancelTxId(), bizTx.getTxId());
                }, customExecutor
        );

        // cancel sku
        CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                productService.cancelUpdateProductStorage(productService.getReserveOrderPatchCommands(command.getProductList()), bizTx.getCancelTxId(), bizTx.getTxId()), customExecutor
        );

        // cancel clear cart
        CompletableFuture<Void> clearCartFuture = CompletableFuture.runAsync(() -> {
                    Set<String> collect = command.getProductList().stream().map(CartDetail::getCartId).collect(Collectors.toSet());
                    cartService.cancelClearCart(command.getUserId(), collect, bizTx.getCancelTxId(), bizTx.getTxId());
                }, customExecutor
        );
        // cancel create order
        CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                orderService.cancelCreateNewOrder(command, bizTx.getCancelTxId(), command.getTxId()), customExecutor
        );

        try {
            paymentQRLinkFuture.get();
            bizTx.getGeneratePaymentLinkTx().setStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException e) {
            log.error("error during payment cancel",e);
            //do nothing
        }
        try {
            decreaseOrderStorageFuture.get();
            bizTx.setDecreaseOrderStorageTxStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException e) {
            log.error("error during order storage cancel",e);
            //do nothing
        }
        try {
            clearCartFuture.get();
            bizTx.setRemoveItemsFromCartStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException e) {
            log.error("error during cart cancel",e);
            //do nothing
        }

        try {
            updateOrderFuture.get();
            bizTx.getCreateOrderTx().setStatus(SubTaskStatus.CANCELLED);
        } catch (InterruptedException | ExecutionException ex) {
            log.error("error during order cancel",ex);
            //do nothing
        }
        if (
                bizTx.getGeneratePaymentLinkTx().getStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getDecreaseOrderStorageTxStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getRemoveItemsFromCartStatus().equals(SubTaskStatus.CANCELLED) &&
                        bizTx.getCreateOrderTx().getStatus().equals(SubTaskStatus.CANCELLED)
        ) {
            bizTx.setTxStatus(TaskStatus.CANCELLED);
        }

        taskRepository.save(bizTx);
    }

}

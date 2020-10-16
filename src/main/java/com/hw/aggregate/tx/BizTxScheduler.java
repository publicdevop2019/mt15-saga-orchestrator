package com.hw.aggregate.tx;

import com.hw.aggregate.sm.CartService;
import com.hw.aggregate.sm.OrderService;
import com.hw.aggregate.sm.PaymentService;
import com.hw.aggregate.sm.ProductService;
import com.hw.aggregate.sm.exception.BizOrderSchedulerTaskRollbackException;
import com.hw.aggregate.tx.model.BizTx;
import com.hw.aggregate.tx.model.BizTxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
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
public class BizTxScheduler {
    @Autowired
    private BizTxRepository taskRepository;
    @Value("${task.expireAfter}")
    private Long taskExpireAfter;
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;

    @Scheduled(fixedRateString = "${fixedRate.in.milliseconds.taskRollback}")
    public void rollbackTask() {
        Date from = Date.from(Instant.ofEpochMilli(Instant.now().toEpochMilli() - taskExpireAfter * 60 * 1000));
        List<BizTx> tasks = taskRepository.findExpiredStartedOrFailTxs(from);
        if (!tasks.isEmpty()) {
            log.info("expired & started task found {}", tasks.stream().map(BizTx::getId).collect(Collectors.toList()));
            tasks.forEach(task -> {
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    Optional<BizTx> byIdOptLock = taskRepository.findByIdOptLock(task.getId());
                                    if (byIdOptLock.isPresent()
                                            && byIdOptLock.get().getCreatedAt().compareTo(from) < 0
                                            && byIdOptLock.get().getTxStatus().equals(BizTxStatus.STARTED)
                                    ) {
                                        rollback(task);
                                    }

                                }
                            });
                    log.info("rollback task {} success", task.getId());
                } catch (Exception e) {
                    log.error("rollback task {} failed", task.getId(), e);
                }
            });
        }
    }

    private void rollback(BizTx transactionalTask) {
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() ->
                paymentService.rollbackTransaction(transactionalTask.getTxId()), customExecutor
        );
        CompletableFuture<Void> voidCompletableFuture1 = CompletableFuture.runAsync(() ->
                productService.rollbackTransaction(transactionalTask.getTxId()), customExecutor
        );
        CompletableFuture<Void> voidCompletableFuture2 = CompletableFuture.runAsync(() ->
                cartService.rollbackTransaction(transactionalTask.getTxId()), customExecutor
        );
        CompletableFuture<Void> voidCompletableFuture3 = CompletableFuture.runAsync(() ->
                orderService.rollbackTransaction(transactionalTask.getTxId()), customExecutor
        );
        CompletableFuture<Void> allOf = CompletableFuture.allOf(voidCompletableFuture, voidCompletableFuture1, voidCompletableFuture2, voidCompletableFuture3);
        try {
            allOf.get();
        } catch (InterruptedException e) {
            log.warn("thread was interrupted", e);
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException e) {
            throw new BizOrderSchedulerTaskRollbackException(e);
        }
        log.info("rollback transaction async call complete");
        transactionalTask.setTxStatus(BizTxStatus.ROLLBACK_ACK);
        transactionalTask.setRollbackReason("Started Expired");
        try {
            taskRepository.saveAndFlush(transactionalTask);
        } catch (Exception e) {
            throw new BizOrderSchedulerTaskRollbackException(e);
        }
    }

}

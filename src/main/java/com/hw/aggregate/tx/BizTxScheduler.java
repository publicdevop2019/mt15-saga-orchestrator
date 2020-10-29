package com.hw.aggregate.tx;

import com.hw.aggregate.sm.exception.BizOrderSchedulerTaskRollbackException;
import com.hw.aggregate.tx.command.AppUpdateBizTxCommand;
import com.hw.aggregate.tx.model.BizTx;
import com.hw.aggregate.tx.model.BizTxStatus;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
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

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.hw.shared.AppConstant.HTTP_HEADER_CHANGE_ID;

@Slf4j
@EnableScheduling
public class BizTxScheduler {
    @Autowired
    private BizTxRepository taskRepository;
    @Value("${task.expireAfter}")
    private Long taskExpireAfter;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;
    @Autowired
    private AppBizTxApplicationService taskService;

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

    private void rollback(BizTx entityRep) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
        ) {
            channel.exchangeDeclare("rollback", "direct");
            String message = HTTP_HEADER_CHANGE_ID + ":" + entityRep.getTxId();
            channel.basicPublish("rollback", "scope:mall", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
            log.info("rollback message sent, updating tx status");
            AppUpdateBizTxCommand appUpdateBizTaskCommand = new AppUpdateBizTxCommand();
            appUpdateBizTaskCommand.setTaskStatus(BizTxStatus.ROLLBACK_ACK);
            appUpdateBizTaskCommand.setRollbackReason("Started Expired");
            taskService.replaceById(entityRep.getId(), appUpdateBizTaskCommand, UUID.randomUUID().toString());
        } catch (TimeoutException | IOException e) {
            log.error("error during rollback message deliver, tx remain fail status", e);
            throw new BizOrderSchedulerTaskRollbackException(e);
        }
    }

}

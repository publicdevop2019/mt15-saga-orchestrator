package com.hw.aggregate.sm;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.exception.BizJobLauncherException;
import com.hw.aggregate.sm.model.CustomStateMachineBuilder;
import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import com.hw.config.batch.ReleaseJobContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hw.aggregate.sm.model.CustomStateMachineEventListener.ERROR_CLASS;

@Slf4j
@Service
public class AppBizStateMachineApplicationService {
    public static final String BIZ_ORDER = "BizOrder";
    @Autowired
    private CustomStateMachineBuilder customStateMachineBuilder;
    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    Job job;
    @Autowired
    ReleaseJobContext releaseJobContext;

    public void start(CreateBizStateMachineCommand command) {
        StateMachine<BizOrderStatus, BizOrderEvent> stateMachine = customStateMachineBuilder.buildMachine(command.getOrderState());
        stateMachine.getExtendedState().getVariables().put(BIZ_ORDER, command);
        if (command.getPrepareEvent() != null) {
            stateMachine.sendEvent(command.getPrepareEvent());
            if (stateMachine.hasStateMachineError()) {
                throw stateMachine.getExtendedState().get(ERROR_CLASS, RuntimeException.class);
            }
        }
        stateMachine.sendEvent(command.getBizOrderEvent());
        if (stateMachine.hasStateMachineError()) {
            throw stateMachine.getExtendedState().get(ERROR_CLASS, RuntimeException.class);
        }
    }

    public void startBatch(List<CreateBizStateMachineCommand> command) {
        try {
            String s = UUID.randomUUID().toString();
            releaseJobContext.getJobList().put(s, command);
            releaseJobContext.getJobIndex().put(s, new AtomicInteger(-1));
            JobParameters paramJobParameters = new JobParametersBuilder().addString("list", s).toJobParameters();
            jobLauncher.run(job, paramJobParameters);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("error during job launch", e);
            throw new BizJobLauncherException();
        }
    }
}

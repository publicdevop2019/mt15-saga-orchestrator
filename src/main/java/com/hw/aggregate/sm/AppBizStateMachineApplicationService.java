package com.hw.aggregate.sm;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.model.CustomStateMachineBuilder;
import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.hw.aggregate.sm.model.CustomStateMachineEventListener.ERROR_CLASS;

@Slf4j
@Service
public class AppBizStateMachineApplicationService {
    public static final String BIZ_ORDER = "BizOrder";
    @Autowired
    private CustomStateMachineBuilder customStateMachineBuilder;

    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;

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
        command.forEach(e -> {
            CompletableFuture<Void> orderFuture = CompletableFuture.runAsync(() ->
                    start(e), customExecutor
            );
        });
    }
}

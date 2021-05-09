package com.mt.saga.infrastructure;

import com.mt.saga.domain.model.order_state_machine.order.BizOrderEvent;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringStateMachineErrorHandler extends StateMachineListenerAdapter<BizOrderStatus, BizOrderEvent> {
    public static final String ERROR_CLASS = "ERROR_CLASS";
    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;

    @Override
    public void stateMachineError(StateMachine<BizOrderStatus, BizOrderEvent> stateMachine, Exception exception) {
        log.error("error during state machine execution {}", getExceptionName(exception));
        //set error class so it can be thrown later, thrown ex here will still result 200 response
        stateMachine.getExtendedState().getVariables().put(ERROR_CLASS, exception);
    }

    private String getExceptionName(Exception exception) {
        String[] split = exception.getClass().getName().split("\\.");
        String s = split[split.length - 1];
        return s;
    }

}

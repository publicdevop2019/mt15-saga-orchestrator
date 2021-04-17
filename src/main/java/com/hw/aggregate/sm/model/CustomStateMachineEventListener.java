package com.hw.aggregate.sm.model;

import com.hw.aggregate.sm.CartService;
import com.hw.aggregate.sm.OrderService;
import com.hw.aggregate.sm.PaymentService;
import com.hw.aggregate.sm.ProductService;
import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import com.hw.aggregate.tx.AppTaskApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomStateMachineEventListener
        extends StateMachineListenerAdapter<BizOrderStatus, BizOrderEvent> {
    public static final String ERROR_CLASS = "ERROR_CLASS";
    //    private final static String QUEUE_NAME = "rollback";
    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductService productService;


    @Autowired
    private AppTaskApplicationService taskService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

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

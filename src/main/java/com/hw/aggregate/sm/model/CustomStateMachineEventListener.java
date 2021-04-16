package com.hw.aggregate.sm.model;

import com.hw.aggregate.sm.CartService;
import com.hw.aggregate.sm.OrderService;
import com.hw.aggregate.sm.PaymentService;
import com.hw.aggregate.sm.ProductService;
import com.hw.aggregate.sm.exception.MultipleStateMachineException;
import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import com.hw.aggregate.tx.AppBizTxApplicationService;
import com.hw.aggregate.tx.representation.AppBizTxRep;
import com.hw.shared.rest.CreatedAggregateRep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static com.hw.aggregate.sm.model.CustomStateMachineBuilder.TX_TASK;

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
    private AppBizTxApplicationService taskService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Override
    public void stateMachineError(StateMachine<BizOrderStatus, BizOrderEvent> stateMachine, Exception exception) {
        log.error("start of stateMachineError, rollback transaction");
        //set error class so it can be thrown later, thrown ex here will still result 200 response
        stateMachine.getExtendedState().getVariables().put(ERROR_CLASS, exception);
        CreatedAggregateRep createdTask = stateMachine.getExtendedState().get(TX_TASK, CreatedAggregateRep.class);
        if (createdTask != null) {
            AppBizTxRep appBizTaskRep = taskService.readById(createdTask.getId());
            String s = getExceptionName(exception);
            if (exception instanceof MultipleStateMachineException) {
                s = ((MultipleStateMachineException) exception).getExs().stream().map(this::getExceptionName).collect(Collectors.joining(","));
            }
        } else {
            log.info("error happened in non-transactional context, no rollback will be triggered");
        }
    }

    private String getExceptionName(Exception exception) {
        String[] split = exception.getClass().getName().split("\\.");
        String s = split[split.length - 1];
        return s;
    }

}

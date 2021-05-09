package com.mt.saga.domain.model.order_state_machine;

import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;

public interface OrderStateMachineBuilder {
    void handleEvent(OrderOperationEvent event);
}

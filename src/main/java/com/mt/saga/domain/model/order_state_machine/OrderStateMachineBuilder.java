package com.mt.saga.domain.model.order_state_machine;

import com.mt.saga.domain.model.order_state_machine.event.UserPlaceOrderEvent;

public interface OrderStateMachineBuilder {
    void handleEvent(UserPlaceOrderEvent event);
}

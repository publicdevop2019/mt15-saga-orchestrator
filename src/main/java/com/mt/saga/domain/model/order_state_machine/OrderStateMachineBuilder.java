package com.mt.saga.domain.model.order_state_machine;

import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;
import com.mt.saga.domain.model.order_state_machine.order.AppCreateBizOrderCommand;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderStatus;

public interface OrderStateMachineBuilder {
    void handleEvent(OrderOperationEvent event);
}

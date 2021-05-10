package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;
import com.mt.saga.domain.model.order_state_machine.order.CartDetail;
import lombok.Getter;

import java.util.List;

import static com.mt.saga.domain.model.order_state_machine.event.create_new_order.ClearCartResultEvent.CREATE_NEW_ORDER_REPLY;
@Getter
public class ValidateProductResultEvent  extends DomainEvent {
    private boolean success;
    private long taskId;
    public ValidateProductResultEvent() {
        setInternal(false);
        setTopic(CREATE_NEW_ORDER_REPLY);
    }
}

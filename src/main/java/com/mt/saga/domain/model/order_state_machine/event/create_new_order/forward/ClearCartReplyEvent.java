package com.mt.saga.domain.model.order_state_machine.event.create_new_order.forward;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;

@Getter
public class ClearCartReplyEvent extends DomainEvent {
    private boolean success;
    private long taskId;

    public ClearCartReplyEvent() {
        setInternal(false);
    }
}

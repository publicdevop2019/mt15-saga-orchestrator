package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;

@Getter
public class ClearCartResultEvent extends DomainEvent {
    public static String CREATE_NEW_ORDER_REPLY = "CREATE_NEW_ORDER_REPLY";
    private boolean success;
    private long taskId;

    public ClearCartResultEvent() {
        setInternal(false);
        setTopic(CREATE_NEW_ORDER_REPLY);
    }
}

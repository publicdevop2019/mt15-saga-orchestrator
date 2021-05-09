package com.mt.saga.domain.model.order_state_machine.event;

import com.mt.common.domain.model.domainId.DomainId;
import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;

@Getter
public class OperationResultEvent extends DomainEvent {
    public static final String TOPIC_ORDER_REPLY = "ORDER_REPLY";
    private final boolean success;

    public OperationResultEvent(String orderId, boolean success) {
        setInternal(false);
        setTopic(TOPIC_ORDER_REPLY);
        setDomainId(new DomainId(orderId));
        this.success = success;
    }
}

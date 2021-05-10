package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;

import static com.mt.saga.domain.model.order_state_machine.event.create_new_order.ClearCartResultEvent.CREATE_NEW_ORDER_REPLY;
@Getter
public class GeneratePaymentQRLinkResultEvent  extends DomainEvent {
    private boolean success;
    private long taskId;
    private String paymentLink;
    public GeneratePaymentQRLinkResultEvent() {
        setInternal(false);
        setTopic(CREATE_NEW_ORDER_REPLY);
    }
}

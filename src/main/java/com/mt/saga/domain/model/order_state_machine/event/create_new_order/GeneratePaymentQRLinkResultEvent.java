package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;

@Getter
public class GeneratePaymentQRLinkResultEvent extends DomainEvent {
    private long taskId;
    private String paymentLink;

    public GeneratePaymentQRLinkResultEvent() {
        setInternal(false);
    }
}

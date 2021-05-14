package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneratePaymentQRLinkEvent extends DomainEvent {
    private String orderId;
    private String changeId;

    public GeneratePaymentQRLinkEvent(String orderId, String taskId) {
        this.orderId = orderId;
        changeId = taskId;
        setInternal(false);
        setTopic("generate_order_payment_link_event");
    }
}

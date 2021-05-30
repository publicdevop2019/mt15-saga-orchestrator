package com.mt.saga.domain.model.order_state_machine.event.create_new_order.forward;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneratePaymentQRLinkEvent extends DomainEvent {
    private String orderId;
    private String changeId;
    private String taskId;

    public GeneratePaymentQRLinkEvent(String orderId, String changeId, String taskId) {
        this.orderId = orderId;
        this.changeId = changeId;
        this.taskId = taskId;
        setInternal(false);
        setTopic("generate_order_payment_link_event");
    }
}

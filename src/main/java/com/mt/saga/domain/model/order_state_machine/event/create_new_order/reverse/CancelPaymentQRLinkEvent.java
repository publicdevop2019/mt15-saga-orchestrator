package com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Setter;

@Setter
public class CancelPaymentQRLinkEvent extends DomainEvent {
    private String orderId;
    private String changeId;
    private String cancelChangeId;
    private long taskId;

    public CancelPaymentQRLinkEvent(String orderId, String changeId, long taskId,String cancelChangeId) {
        this.orderId = orderId;
        this.changeId = changeId;
        this.taskId = taskId;
        this.cancelChangeId = cancelChangeId;
        setInternal(false);
        setTopic("cancel_order_payment_link_event");
    }
}

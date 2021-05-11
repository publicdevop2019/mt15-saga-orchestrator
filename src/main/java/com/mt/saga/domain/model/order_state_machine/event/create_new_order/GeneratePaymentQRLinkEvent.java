package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import static com.mt.saga.domain.model.order_state_machine.event.create_new_order.ClearCartEvent.CREATE_NEW_ORDER;
@Getter
@Setter
public class GeneratePaymentQRLinkEvent extends DomainEvent {
    private String orderId;
    private String changeId;

    public GeneratePaymentQRLinkEvent(String orderId, String taskId) {
        this.orderId = orderId;
        changeId = taskId;
        setInternal(false);
        setTopic(CREATE_NEW_ORDER);
    }
}

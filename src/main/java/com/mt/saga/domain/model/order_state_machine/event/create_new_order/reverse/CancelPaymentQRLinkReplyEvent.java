package com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse;

import lombok.Getter;

@Getter
public class CancelPaymentQRLinkReplyEvent {
    private boolean success;
    private long taskId;
}
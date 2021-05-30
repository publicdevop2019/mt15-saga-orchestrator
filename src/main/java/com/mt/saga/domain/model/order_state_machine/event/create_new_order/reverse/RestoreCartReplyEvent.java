package com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse;

public class RestoreCartReplyEvent {
    private boolean success;
    private long taskId;
}

package com.mt.saga.domain.model.order_state_machine.exception;

public class BizOrderUpdateException extends RuntimeException {
    public BizOrderUpdateException(Throwable cause) {
        super("error during update order", cause);
    }
}

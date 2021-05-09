package com.mt.saga.domain.model.order_state_machine.exception;

public class BizOrderValidationException extends RuntimeException {
    public BizOrderValidationException(Throwable cause) {
        super("error during validate order", cause);
    }
}

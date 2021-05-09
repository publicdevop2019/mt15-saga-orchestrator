package com.mt.saga.domain.model.order_state_machine.exception;

public class BizOrderStorageDecreaseException extends RuntimeException {
    public BizOrderStorageDecreaseException(Throwable cause) {
        super("error during decrease order storage", cause);
    }
}

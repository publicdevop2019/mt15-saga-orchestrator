package com.mt.saga.domain.model.order_state_machine.exception;

public class ActualStorageDecreaseException extends RuntimeException {
    public ActualStorageDecreaseException(Throwable cause) {
        super("error during decrease actual storage", cause);
    }
}

package com.hw.aggregate.sm.exception;

public class BizOrderStorageDecreaseException extends RuntimeException {
    public BizOrderStorageDecreaseException(Throwable cause) {
        super("error during decrease order storage", cause);
    }
}

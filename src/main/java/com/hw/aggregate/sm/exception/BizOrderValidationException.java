package com.hw.aggregate.sm.exception;

public class BizOrderValidationException extends RuntimeException {
    public BizOrderValidationException(Throwable cause) {
        super("error during validate order", cause);
    }
}

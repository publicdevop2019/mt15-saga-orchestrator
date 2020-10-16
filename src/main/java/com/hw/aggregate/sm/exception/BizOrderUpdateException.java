package com.hw.aggregate.sm.exception;

public class BizOrderUpdateException extends RuntimeException {
    public BizOrderUpdateException(Throwable cause) {
        super("error during update order", cause);
    }
}

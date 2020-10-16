package com.hw.aggregate.sm.exception;

public class ActualStorageDecreaseException extends RuntimeException {
    public ActualStorageDecreaseException(Throwable cause) {
        super("error during decrease actual storage", cause);
    }
}

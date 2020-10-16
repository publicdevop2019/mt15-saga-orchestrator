package com.hw.aggregate.sm.exception;

public class StateMachineCreationException extends RuntimeException {
    public StateMachineCreationException(Throwable cause) {
        super("error during creating state machine", cause);
    }
}

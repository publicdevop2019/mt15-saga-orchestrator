package com.mt.saga.domain.model.order_state_machine.exception;

public class StateMachineCreationException extends RuntimeException {
    public StateMachineCreationException(Throwable cause) {
        super("error during creating state machine", cause);
    }
}

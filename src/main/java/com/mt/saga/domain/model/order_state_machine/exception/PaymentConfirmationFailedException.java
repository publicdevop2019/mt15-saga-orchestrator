package com.mt.saga.domain.model.order_state_machine.exception;

public class PaymentConfirmationFailedException extends RuntimeException {
    public PaymentConfirmationFailedException(Throwable cause) {
        super("error during payment confirmation", cause);
    }

    public PaymentConfirmationFailedException() {
        super("payment confirmation failed");
    }
}

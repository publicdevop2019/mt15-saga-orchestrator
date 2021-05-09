package com.mt.saga.domain.model.order_state_machine.exception;

public class PaymentQRLinkGenerationException extends RuntimeException {
    public PaymentQRLinkGenerationException(Throwable cause) {
        super("error during generating payment link", cause);
    }
}

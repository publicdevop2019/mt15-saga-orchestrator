package com.hw.aggregate.sm.exception;

public class PaymentQRLinkGenerationException extends RuntimeException {
    public PaymentQRLinkGenerationException(Throwable cause) {
        super("error during generating payment link", cause);
    }
}

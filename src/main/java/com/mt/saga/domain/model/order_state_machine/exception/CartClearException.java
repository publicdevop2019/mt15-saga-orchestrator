package com.mt.saga.domain.model.order_state_machine.exception;

public class CartClearException extends RuntimeException {
    public CartClearException(Throwable cause) {
        super("error during clear cart", cause);
    }
}

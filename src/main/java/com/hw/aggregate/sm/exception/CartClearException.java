package com.hw.aggregate.sm.exception;

public class CartClearException extends RuntimeException {
    public CartClearException(Throwable cause) {
        super("error during clear cart", cause);
    }
}

package com.hw.aggregate.tx.exception;

public class BizTxPersistenceException extends RuntimeException {
    public BizTxPersistenceException(Throwable cause) {
        super("error during saving biz tx entity", cause);
    }
}

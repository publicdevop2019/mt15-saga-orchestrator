package com.mt.saga.domain.model.task;

public class BizTxPersistenceException extends RuntimeException {
    public BizTxPersistenceException(Throwable cause) {
        super("error during saving biz tx entity", cause);
    }
}

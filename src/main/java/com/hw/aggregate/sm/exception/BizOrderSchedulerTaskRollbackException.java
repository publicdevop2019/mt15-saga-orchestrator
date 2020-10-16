package com.hw.aggregate.sm.exception;

public class BizOrderSchedulerTaskRollbackException extends RuntimeException{
    public BizOrderSchedulerTaskRollbackException(Throwable cause) {
        super("error during rollback transaction", cause);
    }
}

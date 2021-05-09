package com.mt.saga.domain.model.order_state_machine.exception;

public class BizOrderSchedulerTaskRollbackException extends RuntimeException{
    public BizOrderSchedulerTaskRollbackException(Throwable cause) {
        super("error during rollback transaction", cause);
    }
}

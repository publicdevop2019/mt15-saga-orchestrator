package com.mt.saga.domain.model.order_state_machine.exception;

import lombok.Data;

import java.util.List;
@Data
public class MultipleStateMachineException extends RuntimeException {
    private List<RuntimeException> exs;

    public MultipleStateMachineException(List<RuntimeException> exs) {
        this.exs = exs;
    }
}

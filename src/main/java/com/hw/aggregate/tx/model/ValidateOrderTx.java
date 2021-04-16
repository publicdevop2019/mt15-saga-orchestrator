package com.hw.aggregate.tx.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
@Setter
@Getter
public class ValidateOrderTx {
    @Column(name = "validateStatus")
    private SubTxStatus status = SubTxStatus.STARTED;
    @Column(name = "validateResult")
    private Boolean result;

    public ValidateOrderTx() {
    }
}

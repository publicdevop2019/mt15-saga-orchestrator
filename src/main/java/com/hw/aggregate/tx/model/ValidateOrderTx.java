package com.hw.aggregate.tx.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;

@Setter
@Getter
public class ValidateOrderTx {
    @Column(name = "validateStatus")
    @Convert(converter = SubTxStatus.DBConverter.class)
    private SubTxStatus status = SubTxStatus.STARTED;
    @Column(name = "validateResult")
    private Boolean result;

    public ValidateOrderTx() {
    }
}

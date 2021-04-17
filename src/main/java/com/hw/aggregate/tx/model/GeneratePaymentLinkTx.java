package com.hw.aggregate.tx.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;

@Setter
@Getter
public class GeneratePaymentLinkTx {
    @Column(name = "generatePaymentLinkStatus")
    @Convert(converter = SubTxStatus.DBConverter.class)
    private SubTxStatus status = SubTxStatus.STARTED;
    @Column(name = "generatePaymentLinkResults")
    private String results;

    public GeneratePaymentLinkTx() {
    }
}

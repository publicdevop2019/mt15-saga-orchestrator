package com.hw.aggregate.tx.model;

import javax.persistence.Column;

public class GeneratePaymentLinkTx {
    @Column(name = "generatePaymentLinkStatus")
    private SubTxStatus status = SubTxStatus.STARTED;
    @Column(name = "generatePaymentLinkResults")
    private String results;

    public GeneratePaymentLinkTx() {
    }
}

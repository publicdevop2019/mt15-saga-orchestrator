package com.hw.aggregate.tx.model;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;

import javax.persistence.Column;

public class CreateOrderTx {
    @Column(name = "createOrderStatus")
    private SubTxStatus status = SubTxStatus.STARTED;
    @Column(name = "createOrderResult")
    private boolean result;

    public CreateOrderTx() {
    }
}

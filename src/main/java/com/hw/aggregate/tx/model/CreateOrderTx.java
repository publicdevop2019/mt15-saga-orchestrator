package com.hw.aggregate.tx.model;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;

@Setter
@Getter
public class CreateOrderTx {
    @Column(name = "createOrderStatus")
    @Convert(converter = SubTxStatus.DBConverter.class)
    private SubTxStatus status = SubTxStatus.STARTED;
    @Column(name = "createOrderResult")
    private boolean result;

    public CreateOrderTx() {
    }
}

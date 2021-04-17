package com.hw.aggregate.tx.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;

@Setter
@Getter
public class GeneratePaymentLinkSubTask {
    @Column(name = "generatePaymentLinkStatus")
    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus status = SubTaskStatus.STARTED;
    @Column(name = "generatePaymentLinkResults")
    private String results;

    public GeneratePaymentLinkSubTask() {
    }
}

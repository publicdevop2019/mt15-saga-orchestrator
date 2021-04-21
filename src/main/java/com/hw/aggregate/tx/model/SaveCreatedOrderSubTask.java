package com.hw.aggregate.tx.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;

@Setter
@Getter
public class SaveCreatedOrderSubTask {
    @Column(name = "createOrderSubTaskStatus")
    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus status = SubTaskStatus.STARTED;
    @Column(name = "createOrderSubTaskResult")
    private boolean result;

    public SaveCreatedOrderSubTask() {
    }
}

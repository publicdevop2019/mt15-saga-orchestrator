package com.mt.saga.domain.model.task.create_order_task;

import com.mt.saga.domain.model.task.SubTaskStatus;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;

@Setter
@Getter
public class ValidateOrderSubTask {
    @Column(name = "validateStatus")
    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus status = SubTaskStatus.STARTED;
    @Column(name = "validateResult")
    private Boolean result;

    public ValidateOrderSubTask() {
    }
}

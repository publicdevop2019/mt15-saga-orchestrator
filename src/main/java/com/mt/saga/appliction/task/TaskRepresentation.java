package com.mt.saga.appliction.task;

import com.mt.saga.domain.model.task.create_order_task.CreateOrderTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class TaskRepresentation {

    private String transactionId;
    private Long id;

    public TaskRepresentation(CreateOrderTask bizTask) {
        BeanUtils.copyProperties(bizTask, this);
        this.transactionId = bizTask.getChangeId();
    }

}

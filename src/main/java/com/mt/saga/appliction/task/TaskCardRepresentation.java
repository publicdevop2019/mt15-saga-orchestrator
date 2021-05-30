package com.mt.saga.appliction.task;

import com.mt.saga.domain.model.task.TaskName;
import com.mt.saga.domain.model.task.TaskStatus;
import com.mt.saga.domain.model.task.create_order_task.CreateOrderTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class TaskCardRepresentation {
    private Long id;
    private TaskName taskName;
    private TaskStatus taskStatus;
    private String transactionId;
    private Long referenceId;
    private String rollbackReason;
    private Integer version;
    private String createdBy;
    private long createdAt;
    private String modifiedBy;
    private long modifiedAt;

    public TaskCardRepresentation(CreateOrderTask bizTask) {
        BeanUtils.copyProperties(bizTask, this);
        this.taskName = bizTask.getTaskName();
        this.taskStatus = bizTask.getTaskStatus();
        this.transactionId = bizTask.getForwardChangeId();
    }
}

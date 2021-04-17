package com.hw.aggregate.tx.representation;

import com.hw.aggregate.tx.model.CreateOrderTask;
import com.hw.aggregate.tx.model.TaskName;
import com.hw.aggregate.tx.model.TaskStatus;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class AdminBizTxCardRep {
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

    public AdminBizTxCardRep(CreateOrderTask bizTask) {
        BeanUtils.copyProperties(bizTask, this);
        this.taskName = bizTask.getTxName();
        this.taskStatus = bizTask.getTxStatus();
        this.transactionId = bizTask.getTxId();
    }
}

package com.hw.aggregate.tx.representation;

import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.tx.model.BizTx;
import com.hw.aggregate.tx.model.BizTxStatus;
import lombok.Data;

@Data
public class AdminBizTxCardRep {
    private Long id;
    private BizOrderEvent taskName;
    private BizTxStatus taskStatus;
    private String transactionId;
    private Long referenceId;
    private String rollbackReason;
    private Integer version;
    private String createdBy;
    private long createdAt;
    private String modifiedBy;
    private long modifiedAt;

    public AdminBizTxCardRep(BizTx bizTask) {

        this.id = bizTask.getId();
        this.taskName = bizTask.getTxName();
        this.taskStatus = bizTask.getTxStatus();
        this.transactionId = bizTask.getTxId();
        this.rollbackReason = bizTask.getRollbackReason();
        this.referenceId = bizTask.getReferenceId();
        this.version = bizTask.getVersion();
        this.createdBy = bizTask.getCreatedBy();
        this.createdAt = bizTask.getCreatedAt().getTime();
        this.modifiedBy = bizTask.getModifiedBy();
        this.modifiedAt = bizTask.getModifiedAt().getTime();
    }
}

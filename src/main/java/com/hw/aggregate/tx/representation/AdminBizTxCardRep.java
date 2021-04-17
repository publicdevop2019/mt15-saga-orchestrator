package com.hw.aggregate.tx.representation;

import com.hw.aggregate.tx.model.CreateOrderBizTx;
import com.hw.aggregate.tx.model.TxName;
import com.hw.aggregate.tx.model.TxStatus;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class AdminBizTxCardRep {
    private Long id;
    private TxName taskName;
    private TxStatus taskStatus;
    private String transactionId;
    private Long referenceId;
    private String rollbackReason;
    private Integer version;
    private String createdBy;
    private long createdAt;
    private String modifiedBy;
    private long modifiedAt;

    public AdminBizTxCardRep(CreateOrderBizTx bizTask) {
        BeanUtils.copyProperties(bizTask, this);
        this.taskName = bizTask.getTxName();
        this.taskStatus = bizTask.getTxStatus();
        this.transactionId = bizTask.getTxId();
    }
}

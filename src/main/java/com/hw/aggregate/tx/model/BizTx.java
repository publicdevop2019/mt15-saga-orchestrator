package com.hw.aggregate.tx.model;

import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.tx.command.AppCreateBizTxCommand;
import com.hw.aggregate.tx.command.AppUpdateBizTxCommand;
import com.hw.shared.Auditable;
import com.hw.shared.rest.IdBasedEntity;
import com.hw.shared.rest.VersionBasedEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table
@Data
@NoArgsConstructor
public class BizTx extends Auditable implements IdBasedEntity, Serializable , VersionBasedEntity {
    @Id
    private Long id;

    @Column(length = 50)
    @Convert(converter = BizOrderEvent.DBConverter.class)
    private BizOrderEvent taskName;
    public static final String ENTITY_TASK_NAME = "taskName";

    @Column(length = 25)
    @Convert(converter = BizTxStatus.DBConverter.class)
    private BizTxStatus taskStatus;
    public static final String ENTITY_TASK_STATUS = "taskStatus";

    private String transactionId;
    private String rollbackReason;
    private Long referenceId;
    public static final String ENTITY_REFERENCE_ID = "referenceId";

    @Version
    private Integer version;

    public static BizTx create(Long id, AppCreateBizTxCommand command) {
        return new BizTx(id, command);
    }

    public BizTx(Long id, AppCreateBizTxCommand command) {
        this.id = id;
        this.taskName = command.getTaskName();
        this.taskStatus = BizTxStatus.STARTED;
        this.transactionId = command.getTransactionId();
        this.referenceId = command.getReferenceId();
    }

    public BizTx replace(AppUpdateBizTxCommand command) {
        this.setTaskStatus(command.getTaskStatus());
        this.setRollbackReason(command.getRollbackReason());
        return this;
    }
}

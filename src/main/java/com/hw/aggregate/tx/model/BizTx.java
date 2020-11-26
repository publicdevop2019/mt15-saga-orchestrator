package com.hw.aggregate.tx.model;

import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.tx.command.AppCreateBizTxCommand;
import com.hw.aggregate.tx.command.AppUpdateBizTxCommand;
import com.hw.shared.Auditable;
import com.hw.shared.rest.Aggregate;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table
@Data
@NoArgsConstructor
public class BizTx extends Auditable implements Aggregate, Serializable {
    @Id
    private Long id;

    @Column(length = 50)
    @Convert(converter = BizOrderEvent.DBConverter.class)
    private BizOrderEvent txName;
    public static final String ENTITY_TX_NAME = "txName";

    @Column(length = 25)
    @Convert(converter = BizTxStatus.DBConverter.class)
    private BizTxStatus txStatus;
    public static final String ENTITY_TX_STATUS = "txStatus";

    private String txId;
    private String rollbackReason;
    private Long referenceId;
    @Version
    @Setter(AccessLevel.NONE)
    private Integer version;
    public static final String ENTITY_REFERENCE_ID = "referenceId";

    public static BizTx create(Long id, AppCreateBizTxCommand command) {
        return new BizTx(id, command);
    }

    public BizTx(Long id, AppCreateBizTxCommand command) {
        this.id = id;
        this.txName = command.getTaskName();
        this.txStatus = BizTxStatus.STARTED;
        this.txId = command.getTransactionId();
        this.referenceId = command.getReferenceId();
    }

    public BizTx replace(AppUpdateBizTxCommand command) {
        this.setTxStatus(command.getTaskStatus());
        this.setRollbackReason(command.getRollbackReason());
        return this;
    }
}

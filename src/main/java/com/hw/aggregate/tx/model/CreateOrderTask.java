package com.hw.aggregate.tx.model;

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
public class CreateOrderTask extends Auditable implements Aggregate, Serializable {
    public static final String ENTITY_TX_NAME = "txName";
    public static final String ENTITY_TX_STATUS = "txStatus";
    public static final String ENTITY_REFERENCE_ID = "referenceId";
    @Id
    private Long id;

    @Column(length = 50)
    @Convert(converter = TaskName.DBConverter.class)
    private TaskName txName;

    @Column(length = 25)
    @Convert(converter = TaskStatus.DBConverter.class)
    private TaskStatus txStatus;

    private String txId;

    private String cancelTxId;
    private boolean cancelBlocked=false;
    @Lob
    private String createBizStateMachineCommand;
    @Embedded
    private SaveCreatedOrderSubTask createOrderTx;

    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus decreaseOrderStorageTxStatus = SubTaskStatus.STARTED;

    @Embedded
    private GeneratePaymentLinkSubTask generatePaymentLinkTx;

    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus removeItemsFromCartStatus = SubTaskStatus.STARTED;
    @Embedded
    private ValidateOrderSubTask validateOrderTx;

    @Version
    @Setter(AccessLevel.NONE)
    private Integer version;

    public static CreateOrderTask createTask(Long id, String command, String changeId) {
        return new CreateOrderTask(id, command, changeId);
    }

    public CreateOrderTask(Long id, String command, String changeId) {
        this.id = id;
        this.txName = TaskName.CREATE_ORDER;
        this.txStatus = TaskStatus.STARTED;
        this.txId = changeId;
        this.cancelTxId = changeId + "_cancel";
        createBizStateMachineCommand = command;
        createOrderTx = new SaveCreatedOrderSubTask();
        generatePaymentLinkTx = new GeneratePaymentLinkSubTask();
        validateOrderTx = new ValidateOrderSubTask();
    }


}

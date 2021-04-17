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
public class CreateOrderBizTx extends Auditable implements Aggregate, Serializable {
    public static final String ENTITY_TX_NAME = "txName";
    public static final String ENTITY_TX_STATUS = "txStatus";
    public static final String ENTITY_REFERENCE_ID = "referenceId";
    @Id
    private Long id;

    @Column(length = 50)
    @Convert(converter = TxName.DBConverter.class)
    private TxName txName;

    @Column(length = 25)
    @Convert(converter = TxStatus.DBConverter.class)
    private TxStatus txStatus;

    private String txId;

    private String cancelTxId;
    @Lob
    private String createBizStateMachineCommand;
    @Embedded
    private CreateOrderTx createOrderTx;

    @Convert(converter = SubTxStatus.DBConverter.class)
    private SubTxStatus decreaseOrderStorageTxStatus = SubTxStatus.STARTED;

    @Embedded
    private GeneratePaymentLinkTx generatePaymentLinkTx;

    @Convert(converter = SubTxStatus.DBConverter.class)
    private SubTxStatus removeItemsFromCartStatus = SubTxStatus.STARTED;
    @Embedded
    private ValidateOrderTx validateOrderTx;

    @Version
    @Setter(AccessLevel.NONE)
    private Integer version;

    public static CreateOrderBizTx createTx(Long id, String command, String changeId) {
        return new CreateOrderBizTx(id, command, changeId);
    }

    public CreateOrderBizTx(Long id, String command, String changeId) {
        this.id = id;
        this.txName = TxName.CREATE_ORDER;
        this.txStatus = TxStatus.STARTED;
        this.txId = changeId;
        this.cancelTxId = changeId + "_cancel";
        createBizStateMachineCommand = command;
        createOrderTx = new CreateOrderTx();
        generatePaymentLinkTx = new GeneratePaymentLinkTx();
        validateOrderTx = new ValidateOrderTx();
    }


}

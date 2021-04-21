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
    private TaskName taskName;

    @Column(length = 25)
    @Convert(converter = TaskStatus.DBConverter.class)
    private TaskStatus taskStatus;

    private String taskId;
    private String orderId;

    private String cancelTaskId;
    private boolean cancelBlocked = false;
    @Lob
    private String createBizStateMachineCommand;
    @Embedded
    private SaveCreatedOrderSubTask createOrderSubTask;

    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus decreaseOrderStorageSubTaskStatus = SubTaskStatus.STARTED;

    @Embedded
    private GeneratePaymentLinkSubTask generatePaymentLinkSubTask;

    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus removeItemsFromCartSubTaskStatus = SubTaskStatus.STARTED;
    @Embedded
    private ValidateOrderSubTask validateOrderSubTask;

    @Version
    @Setter(AccessLevel.NONE)
    private Integer version;

    public static CreateOrderTask createTask(Long id, String command, String changeId, String orderId) {
        return new CreateOrderTask(id, command, changeId, orderId);
    }

    public CreateOrderTask(Long id, String command, String changeId, String orderId) {
        this.id = id;
        this.taskName = TaskName.CREATE_ORDER;
        this.taskStatus = TaskStatus.STARTED;
        this.taskId = changeId;
        this.orderId = orderId;
        this.cancelTaskId = changeId + "_cancel";
        createBizStateMachineCommand = command;
        createOrderSubTask = new SaveCreatedOrderSubTask();
        generatePaymentLinkSubTask = new GeneratePaymentLinkSubTask();
        validateOrderSubTask = new ValidateOrderSubTask();
    }


}

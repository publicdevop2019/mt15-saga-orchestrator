package com.mt.saga.domain.model.task.create_order_task;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.audit.Auditable;
import com.mt.saga.domain.model.task.*;
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
public class CreateOrderTask extends Auditable implements Serializable {
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

    public CreateOrderTask(String command, String changeId, String orderId) {
        this.id = CommonDomainRegistry.getUniqueIdGeneratorService().id();
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

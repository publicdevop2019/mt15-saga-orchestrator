package com.mt.saga.domain.model.task.create_order_task;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.saga.domain.model.task.SubTaskStatus;
import com.mt.saga.domain.model.task.TaskName;
import com.mt.saga.domain.model.task.TaskStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table
@Data
@NoArgsConstructor
public class CreateOrderTask implements Serializable {
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

    private String forwardChangeId;
    private String orderId;

    private String reverseChangeId;
    private boolean acknowledged = false;
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
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public CreateOrderTask(String command, String changeId, String orderId) {
        this.id = CommonDomainRegistry.getUniqueIdGeneratorService().id();
        this.taskName = TaskName.CREATE_ORDER;
        this.taskStatus = TaskStatus.STARTED;
        this.forwardChangeId = changeId;
        this.orderId = orderId;
        this.reverseChangeId = changeId + "_cancel";
        createBizStateMachineCommand = command;
        createOrderSubTask = new SaveCreatedOrderSubTask();
        generatePaymentLinkSubTask = new GeneratePaymentLinkSubTask();
        validateOrderSubTask = new ValidateOrderSubTask();
    }


    public void checkAllSubTaskStatus() {
        if(createOrderSubTask.getStatus().equals(SubTaskStatus.COMPLETED)
                && decreaseOrderStorageSubTaskStatus.equals(SubTaskStatus.COMPLETED)
                && removeItemsFromCartSubTaskStatus.equals(SubTaskStatus.COMPLETED)
                && validateOrderSubTask.getStatus().equals(SubTaskStatus.COMPLETED)
                && generatePaymentLinkSubTask.getStatus().equals(SubTaskStatus.COMPLETED)){
            setTaskStatus(TaskStatus.COMPLETED);
        }
        if(createOrderSubTask.getStatus().equals(SubTaskStatus.FAILED)
                || decreaseOrderStorageSubTaskStatus.equals(SubTaskStatus.FAILED)
                || removeItemsFromCartSubTaskStatus.equals(SubTaskStatus.FAILED)
                || validateOrderSubTask.getStatus().equals(SubTaskStatus.FAILED)
                || generatePaymentLinkSubTask.getStatus().equals(SubTaskStatus.FAILED)){
            setTaskStatus(TaskStatus.FAILED);
        }
        if(createOrderSubTask.getStatus().equals(SubTaskStatus.CANCELLED)
                && decreaseOrderStorageSubTaskStatus.equals(SubTaskStatus.CANCELLED)
                && removeItemsFromCartSubTaskStatus.equals(SubTaskStatus.CANCELLED)
                && validateOrderSubTask.getStatus().equals(SubTaskStatus.CANCELLED)
                && generatePaymentLinkSubTask.getStatus().equals(SubTaskStatus.CANCELLED)){
            setTaskStatus(TaskStatus.CANCELLED);
        }
    }
}

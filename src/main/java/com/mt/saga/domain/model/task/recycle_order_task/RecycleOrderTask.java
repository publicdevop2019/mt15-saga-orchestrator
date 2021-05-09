package com.mt.saga.domain.model.task.recycle_order_task;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.audit.Auditable;
import com.mt.saga.domain.model.task.SubTaskStatus;
import com.mt.saga.domain.model.task.TaskName;
import com.mt.saga.domain.model.task.TaskStatus;
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
public class RecycleOrderTask extends Auditable implements Serializable {
    public static final String ENTITY_TX_NAME = "txName";
    public static final String ENTITY_TX_STATUS = "txStatus";
    public static final String ENTITY_REFERENCE_ID = "referenceId";
    @Id
    private Long id;

    @Column(length = 50)
    @Convert(converter = TaskName.DBConverter.class)
    private TaskName taskName = TaskName.RECYCLE_ORDER;

    @Column(length = 25)
    @Convert(converter = TaskStatus.DBConverter.class)
    private TaskStatus taskStatus;

    private String taskId;

    private String cancelTaskId;
    private boolean cancelBlocked=false;
    @Lob
    private String createBizStateMachineCommand;

    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus increaseOrderStorageSubTaskStatus = SubTaskStatus.STARTED;
    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus updateOrderSubTaskStatus = SubTaskStatus.STARTED;

    private String orderId;

    public RecycleOrderTask(String command, String changeId, String orderId) {
        this.id = CommonDomainRegistry.getUniqueIdGeneratorService().id();
        this.taskStatus = TaskStatus.STARTED;
        this.taskId = changeId;
        this.orderId = orderId;
        this.cancelTaskId = changeId + "_cancel";
        createBizStateMachineCommand = command;
    }


}

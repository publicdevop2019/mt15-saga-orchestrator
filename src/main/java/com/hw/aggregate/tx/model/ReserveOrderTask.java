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
public class ReserveOrderTask extends Auditable implements Aggregate, Serializable {
    public static final String ENTITY_TX_NAME = "txName";
    public static final String ENTITY_TX_STATUS = "txStatus";
    public static final String ENTITY_REFERENCE_ID = "referenceId";
    @Id
    private Long id;

    @Column(length = 50)
    @Convert(converter = TaskName.DBConverter.class)
    private TaskName taskName = TaskName.RESERVE_ORDER;

    @Column(length = 25)
    @Convert(converter = TaskStatus.DBConverter.class)
    private TaskStatus taskStatus;

    private String taskId;

    private String cancelTaskId;
    private boolean cancelBlocked=false;
    @Lob
    private String createBizStateMachineCommand;

    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus decreaseOrderStorageSubTaskStatus = SubTaskStatus.STARTED;
    @Convert(converter = SubTaskStatus.DBConverter.class)
    private SubTaskStatus updateOrderSubTaskStatus = SubTaskStatus.STARTED;
    @Version
    @Setter(AccessLevel.NONE)
    private Integer version;

    public static ReserveOrderTask createTask(Long id, String command, String changeId) {
        return new ReserveOrderTask(id, command, changeId);
    }

    public ReserveOrderTask(Long id, String command, String changeId) {
        this.id = id;
        this.taskStatus = TaskStatus.STARTED;
        this.taskId = changeId;
        this.cancelTaskId = changeId + "_cancel";
        createBizStateMachineCommand = command;
    }


}

package com.hw.aggregate.task.command;

import com.hw.aggregate.task.model.BizTaskStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppUpdateBizTaskCommand implements Serializable {
    private static final long serialVersionUID = 1;
    private BizTaskStatus taskStatus;
    private String rollbackReason;
}

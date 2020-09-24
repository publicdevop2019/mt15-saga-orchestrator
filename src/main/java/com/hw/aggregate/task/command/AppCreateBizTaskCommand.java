package com.hw.aggregate.task.command;

import com.hw.aggregate.sm.model.BizOrderEvent;
import lombok.Data;

@Data
public class AppCreateBizTaskCommand {
    private BizOrderEvent taskName;
    private String transactionId;
    private Long referenceId;
}

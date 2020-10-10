package com.hw.aggregate.task.command;

import com.hw.aggregate.sm.model.order.BizOrderEvent;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppCreateBizTaskCommand implements Serializable {
    private static final long serialVersionUID = 1;
    private BizOrderEvent taskName;
    private String transactionId;
    private Long referenceId;
}

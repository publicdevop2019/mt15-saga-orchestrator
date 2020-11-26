package com.hw.aggregate.tx.command;

import com.hw.aggregate.tx.model.BizTxStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppUpdateBizTxCommand implements Serializable {
    private static final long serialVersionUID = 1;
    private BizTxStatus taskStatus;
    private String rollbackReason;
    private Integer version;
}

package com.hw.aggregate.sm.command;

import com.hw.aggregate.sm.model.BizOrderEvent;
import com.hw.aggregate.sm.model.BizOrderStatus;
import com.hw.shared.sql.PatchCommand;
import lombok.Data;

import java.util.List;

@Data
public class CreateBizStateMachineCommand {
    private long orderId;
    private long userId;
    private BizOrderStatus orderState;
    private BizOrderEvent bizOrderEvent;
    private BizOrderEvent prepareEvent;
    private String createdBy;
    private List<PatchCommand> orderStorageChange;
    private List<PatchCommand> actualStorageChange;
}

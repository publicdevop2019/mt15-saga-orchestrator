package com.hw.aggregate.sm.command;

import com.hw.aggregate.sm.model.order.BizOrderAddressCmdRep;
import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.sm.model.order.BizOrderItem;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import com.hw.shared.sql.PatchCommand;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreateBizStateMachineCommand {
    private long orderId;
    private String txId;
    private long userId;
    private BizOrderStatus orderState;
    private BizOrderEvent bizOrderEvent;
    private BizOrderEvent prepareEvent;
    private String createdBy;
    private List<PatchCommand> orderStorageChange;
    private List<PatchCommand> actualStorageChange;
    private List<BizOrderItem> productList;
    private BizOrderAddressCmdRep address;
    private String paymentType;
    private BigDecimal paymentAmt;
}

package com.hw.aggregate.sm.command;

import com.hw.aggregate.sm.model.order.BizOrderAddressCmdRep;
import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.sm.model.order.CartDetail;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import com.hw.shared.sql.PatchCommand;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateBizStateMachineCommand implements Serializable {
    private static final long serialVersionUID = 1;
    private String orderId;
    private String txId;
    private String userId;
    private BizOrderStatus orderState;
    private BizOrderEvent bizOrderEvent;
    private BizOrderEvent prepareEvent;
    private String createdBy;
    private List<CartDetail> productList;
    private BizOrderAddressCmdRep address;
    private String paymentType;
    private BigDecimal paymentAmt;
    private Integer version;
}

package com.hw.aggregate.sm.model.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AppCreateBizOrderCommand {
    private long orderId;
    private long userId;
    private BizOrderStatus orderState;
    private String createdBy;
    private BizOrderAddressCmdRep address;
    private List<BizOrderItem> productList;
    private String paymentType;
    private BigDecimal paymentAmt;
    private String paymentLink;
}

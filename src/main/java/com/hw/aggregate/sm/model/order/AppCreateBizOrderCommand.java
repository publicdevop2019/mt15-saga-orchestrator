package com.hw.aggregate.sm.model.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AppCreateBizOrderCommand {
    private String orderId;
    private String userId;
    private BizOrderStatus orderState;
    private String createdBy;
    private BizOrderAddressCmdRep address;
    private List<CartDetail> productList;
    private String paymentType;
    private BigDecimal paymentAmt;
    private String paymentLink;
}

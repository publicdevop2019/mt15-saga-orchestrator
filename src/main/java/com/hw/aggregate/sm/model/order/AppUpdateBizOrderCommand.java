package com.hw.aggregate.sm.model.order;

import lombok.Data;

@Data
public class AppUpdateBizOrderCommand {
    private String orderId;
    private Boolean paymentStatus;
    private BizOrderStatus orderState;
    private Integer version;
}

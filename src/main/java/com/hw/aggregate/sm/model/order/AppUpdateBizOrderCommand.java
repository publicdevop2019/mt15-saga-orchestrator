package com.hw.aggregate.sm.model.order;

import lombok.Data;

@Data
public class AppUpdateBizOrderCommand {
    private long orderId;
    private Boolean paymentStatus;
    private BizOrderStatus orderState;
}

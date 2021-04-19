package com.hw.aggregate.sm.model.order;

import lombok.Getter;
import lombok.Setter;

@Getter
public class CancelUpdateBizOrderCommand {
    private static final long serialVersionUID = 1;
    @Setter
    private String orderId;
    @Setter
    private String changeId;
    @Setter
    private String cancelChangeId;

}

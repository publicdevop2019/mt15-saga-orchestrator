package com.mt.saga.domain.model.order_state_machine.order;

import lombok.Data;

@Data
public class UpdateBizOrderCommand {
    private static final long serialVersionUID = 1;
    private String orderId;
    private Integer version;
    private String changeId;
    private Boolean orderStorage;
    private Boolean actualStorage;
    private Boolean concluded;
    private Boolean cancelled;
    private Boolean paid;
    private Boolean deleted;
    private String deletedBy;
}

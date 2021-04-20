package com.hw.aggregate.sm.model.order;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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

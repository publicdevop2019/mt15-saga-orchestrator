package com.hw.aggregate.sm.model.order;

import lombok.Getter;
import lombok.Setter;

@Getter
public class UpdateBizOrderCommand {
    private static final long serialVersionUID = 1;
    @Setter
    private String orderId;
    @Setter
    private Integer version;
    @Setter
    private String changeId;
    @Setter
    private Boolean orderStorage;
    @Setter
    private Boolean actualStorage;
    @Setter
    private Boolean concluded;
    @Setter
    private Boolean cancelled;
    @Setter
    private Boolean paid;

    @Override
    public String toString() {
        return "UpdateBizOrderCommand{" +
                "orderId=" + orderId +
                ", version=" + version +
                ", changeId='" + changeId + '\'' +
                '}';
    }
}

package com.hw.aggregate.sm.model.order;

import lombok.Data;
import lombok.Setter;

@Data
public class AppUpdateBizOrderCommand {
    @Setter
    private String orderId;
    private Integer version;
    @Setter
    private String changeId;

    @Setter
    private CommandType commandType;

    public enum CommandType {
        CANCEL_CREATE,
        CANCEL_CONCLUDE,
        CANCEL_CONFIRM_PAYMENT,
        CANCEL_RESERVE,
        CANCEL_RECYCLE,
        CONCLUDE,
        CONFIRM_PAYMENT,
        RESERVE,
        USER_DELETE,
        RECYCLE;
    }
}

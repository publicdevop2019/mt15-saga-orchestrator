package com.mt.saga.domain.model.order_state_machine.order;

import com.mt.common.domain.model.sql.converter.EnumConverter;

public enum BizOrderStatus {
    NOT_PAID_RESERVED,
    NOT_PAID_RECYCLED,
    PAID_RESERVED,
    PAID_RECYCLED,
    CONFIRMED,
    DRAFT,
    ;

    public static class DBConverter extends EnumConverter<BizOrderStatus> {
        public DBConverter() {
            super(BizOrderStatus.class);
        }
    }
}

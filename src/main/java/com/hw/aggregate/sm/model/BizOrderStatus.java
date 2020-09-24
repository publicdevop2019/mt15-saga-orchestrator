package com.hw.aggregate.sm.model;

import com.hw.shared.EnumDBConverter;

public enum BizOrderStatus {
    NOT_PAID_RESERVED,
    NOT_PAID_RECYCLED,
    PAID_RESERVED,
    PAID_RECYCLED,
    CONFIRMED,
    DRAFT,
    ;

    public static class DBConverter extends EnumDBConverter<BizOrderStatus> {
        public DBConverter() {
            super(BizOrderStatus.class);
        }
    }
}

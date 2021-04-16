package com.hw.aggregate.tx.model;

import com.hw.shared.EnumDBConverter;

public enum TxName {
    CREATE_ORDER,
    RECYCLE_ORDER,
    CONFIRM_PAYMENT,
    CONCLUDE_ORDER,
    ;

    public static class DBConverter extends EnumDBConverter<TxName> {
        public DBConverter() {
            super(TxName.class);
        }
    }
}

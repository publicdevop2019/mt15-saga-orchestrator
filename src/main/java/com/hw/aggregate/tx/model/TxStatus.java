package com.hw.aggregate.tx.model;

import com.hw.shared.EnumDBConverter;

public enum TxStatus {
    STARTED,
    COMPLETED,
    CANCELLED,
    FAILED;

    public static class DBConverter extends EnumDBConverter<TxStatus> {
        public DBConverter() {
            super(TxStatus.class);
        }
    }
}

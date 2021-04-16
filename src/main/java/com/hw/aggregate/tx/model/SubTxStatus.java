package com.hw.aggregate.tx.model;

import com.hw.shared.EnumDBConverter;

public enum SubTxStatus {
    STARTED,
    COMPLETED,
    CANCELLED,
    TIMEOUT,
    FAILED;

    public static class DBConverter extends EnumDBConverter<SubTxStatus> {
        public DBConverter() {
            super(SubTxStatus.class);
        }
    }
}

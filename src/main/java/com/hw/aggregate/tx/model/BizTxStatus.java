package com.hw.aggregate.tx.model;

import com.hw.shared.EnumDBConverter;

public enum BizTxStatus {
    STARTED,
    ROLLBACK_ACK,
    ROLLBACK_ACK_FAILED,
    COMPLETED,
    FAIL;

    public static class DBConverter extends EnumDBConverter<BizTxStatus> {
        public DBConverter() {
            super(BizTxStatus.class);
        }
    }
}

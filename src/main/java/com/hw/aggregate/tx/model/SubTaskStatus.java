package com.hw.aggregate.tx.model;

import com.hw.shared.EnumDBConverter;

public enum SubTaskStatus {
    STARTED,
    COMPLETED,
    CANCELLED,
    TIMEOUT,
    FAILED;

    public static class DBConverter extends EnumDBConverter<SubTaskStatus> {
        public DBConverter() {
            super(SubTaskStatus.class);
        }
    }
}

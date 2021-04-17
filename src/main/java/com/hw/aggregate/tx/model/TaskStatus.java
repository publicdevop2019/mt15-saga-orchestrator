package com.hw.aggregate.tx.model;

import com.hw.shared.EnumDBConverter;

public enum TaskStatus {
    STARTED,
    COMPLETED,
    CANCELLED,
    FAILED;

    public static class DBConverter extends EnumDBConverter<TaskStatus> {
        public DBConverter() {
            super(TaskStatus.class);
        }
    }
}

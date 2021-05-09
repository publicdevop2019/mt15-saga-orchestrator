package com.mt.saga.domain.model.task;


import com.mt.common.domain.model.sql.converter.EnumConverter;

public enum TaskStatus {
    STARTED,
    COMPLETED,
    CANCELLED,
    FAILED;

    public static class DBConverter extends EnumConverter<TaskStatus> {
        public DBConverter() {
            super(TaskStatus.class);
        }
    }
}

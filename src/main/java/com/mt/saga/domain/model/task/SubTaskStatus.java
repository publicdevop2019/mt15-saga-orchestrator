package com.mt.saga.domain.model.task;


import com.mt.common.domain.model.sql.converter.EnumConverter;

public enum SubTaskStatus {
    STARTED,
    COMPLETED,
    CANCELLED,
    TIMEOUT,
    FAILED;

    public static class DBConverter extends EnumConverter<SubTaskStatus> {
        public DBConverter() {
            super(SubTaskStatus.class);
        }
    }
}

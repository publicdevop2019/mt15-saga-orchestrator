package com.mt.saga.domain.model.task;


import com.mt.common.domain.model.sql.converter.EnumConverter;

public enum TaskName {
    CREATE_ORDER,
    RECYCLE_ORDER,
    CONFIRM_PAYMENT,
    CONCLUDE_ORDER,
    RESERVE_ORDER,
    ;

    public static class DBConverter extends EnumConverter<TaskName> {
        public DBConverter() {
            super(TaskName.class);
        }
    }
}

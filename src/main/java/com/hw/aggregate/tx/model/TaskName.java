package com.hw.aggregate.tx.model;

import com.hw.shared.EnumDBConverter;

public enum TaskName {
    CREATE_ORDER,
    RECYCLE_ORDER,
    CONFIRM_PAYMENT,
    CONCLUDE_ORDER,
    RESERVE_ORDER,
    ;

    public static class DBConverter extends EnumDBConverter<TaskName> {
        public DBConverter() {
            super(TaskName.class);
        }
    }
}

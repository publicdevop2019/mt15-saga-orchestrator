package com.mt.saga.domain.model.order_state_machine.order;

import com.mt.common.domain.model.sql.converter.EnumConverter;

/**
 * use separate prepare event so logic will not miss triggered
 */
public enum BizOrderEvent {
    CONFIRM_PAYMENT,
    CONFIRM_ORDER,
    NEW_ORDER,
    RECYCLE_ORDER_STORAGE,
    PREPARE_CONFIRM_ORDER,
    PREPARE_CONFIRM_PAYMENT,
    PREPARE_RECYCLE_ORDER_STORAGE,
    PREPARE_NEW_ORDER,
    PREPARE_RESERVE,
    RESERVE;

    public static class DBConverter extends EnumConverter<BizOrderEvent> {
        public DBConverter() {
            super(BizOrderEvent.class);
        }
    }

}

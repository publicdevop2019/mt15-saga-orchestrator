package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;

import java.util.Set;

public class ClearCartEvent extends DomainEvent {
    public static String CREATE_NEW_ORDER = "CREATE_NEW_ORDER_COMMAND";
    private String userId;
    private Set<String> ids;
    private String changeId;

    public ClearCartEvent(String userId, Set<String> collect, String changeId) {
        this.userId = userId;
        this.ids = collect;
        this.changeId = changeId;
        setInternal(false);
        setTopic(CREATE_NEW_ORDER);
    }
}

package com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
public class RestoreCartEvent extends DomainEvent {
    private String userId;
    private Set<String> ids;
    private String changeId;
    private String cancel_changeId;
    private long taskId;

    public RestoreCartEvent(String userId, Set<String> collect, String changeId, long taskId, String cancel_changeId) {
        this.userId = userId;
        this.ids = collect;
        this.changeId = changeId;
        this.taskId = taskId;
        this.cancel_changeId = cancel_changeId;
        setInternal(false);
        setTopic("restore_cart_event");
    }
}

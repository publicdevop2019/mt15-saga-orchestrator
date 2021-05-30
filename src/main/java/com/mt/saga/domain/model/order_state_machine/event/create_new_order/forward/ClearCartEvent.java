package com.mt.saga.domain.model.order_state_machine.event.create_new_order.forward;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
@Getter
@Setter
public class ClearCartEvent extends DomainEvent {
    private String userId;
    private Set<String> ids;
    private String changeId;
    private long taskId;

    public ClearCartEvent(String userId, Set<String> collect, String changeId,long taskId) {
        this.userId = userId;
        this.ids = collect;
        this.changeId = changeId;
        this.taskId=taskId;
        setInternal(false);
        setTopic("clear_cart_event");
    }
}

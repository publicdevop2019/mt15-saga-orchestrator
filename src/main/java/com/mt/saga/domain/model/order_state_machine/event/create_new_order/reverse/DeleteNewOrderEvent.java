package com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse;

import com.mt.common.domain.model.domain_event.DomainEvent;
import lombok.Setter;

@Setter
public class DeleteNewOrderEvent extends DomainEvent {
    private String orderId;
    private String userId;
    private String cancelChangeId;
    private String changeId;
    private long taskId;
    private int version;

    public DeleteNewOrderEvent(String orderId, String userId, long taskId, String changeId, String cancelChangeId) {
        setOrderId(orderId);
        setUserId(userId);
        setInternal(false);
        setTopic("delete_new_order_event");
        setTaskId(taskId);
        setChangeId(changeId);
        setCancelChangeId(cancelChangeId);
        setVersion(0);//must be 0 for new order
    }
}

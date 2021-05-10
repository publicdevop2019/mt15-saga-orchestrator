package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;
import com.mt.common.domain.model.restful.PatchCommand;

import java.util.List;

import static com.mt.saga.domain.model.order_state_machine.event.create_new_order.ClearCartEvent.CREATE_NEW_ORDER;

public class DecreaseOrderStorageEvent extends DomainEvent {
    private List<PatchCommand> skuCommands;
    private String changeId;
    public DecreaseOrderStorageEvent(List<PatchCommand> reserveOrderPatchCommands, String taskId) {
        skuCommands=reserveOrderPatchCommands;
        changeId=taskId;
        setInternal(false);
        setTopic(CREATE_NEW_ORDER);
    }
}

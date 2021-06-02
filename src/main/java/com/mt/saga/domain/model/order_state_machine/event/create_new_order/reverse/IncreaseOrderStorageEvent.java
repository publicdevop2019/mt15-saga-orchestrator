package com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse;

import com.mt.common.domain.model.domain_event.DomainEvent;
import com.mt.common.domain.model.restful.PatchCommand;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Setter
@Getter
public class IncreaseOrderStorageEvent extends DomainEvent {
    private List<PatchCommand> skuCommands;
    private String changeId;
    private long taskId;
    public IncreaseOrderStorageEvent(List<PatchCommand> reserveOrderPatchCommands, String changeId, long taskId) {
        skuCommands = reserveOrderPatchCommands;
        this.changeId = changeId;
        this.taskId = taskId;
        setInternal(false);
        setTopic("increase_sku_for_order_event");
    }
}

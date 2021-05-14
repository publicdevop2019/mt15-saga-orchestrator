package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;
import com.mt.saga.domain.model.order_state_machine.order.AppCreateBizOrderCommand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNewOrderEvent extends DomainEvent {
    private AppCreateBizOrderCommand command;
    private String changeId;

    public CreateNewOrderEvent(AppCreateBizOrderCommand paymentLink, String deserialize1) {
        changeId = deserialize1;
        command = paymentLink;
        setInternal(false);
        setTopic("create_new_order_event");
    }
}

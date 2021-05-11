package com.mt.saga.domain.model.order_state_machine.event.create_new_order;

import com.mt.common.domain.model.domain_event.DomainEvent;
import com.mt.saga.domain.model.order_state_machine.order.CartDetail;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static com.mt.saga.domain.model.order_state_machine.event.create_new_order.ClearCartEvent.CREATE_NEW_ORDER;
@Getter
@Setter
public class ValidateProductEvent extends DomainEvent {
    private List<CartDetail> productList;

    public ValidateProductEvent(List<CartDetail> productList) {
        this.productList = productList;
        setInternal(false);
        setTopic(CREATE_NEW_ORDER);
    }
}

package com.mt.saga.appliction.order_state_machine;

import com.mt.common.domain.model.domain_event.SubscribeForEvent;
import com.mt.saga.domain.DomainRegistry;
import com.mt.saga.domain.model.order_state_machine.event.UserPlaceOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OrderStateMachineApplicationService {
    @Transactional
    @SubscribeForEvent
    public void start(UserPlaceOrderEvent event) {
        DomainRegistry.getOrderStateMachineBuilder().handleEvent(event);
    }
}

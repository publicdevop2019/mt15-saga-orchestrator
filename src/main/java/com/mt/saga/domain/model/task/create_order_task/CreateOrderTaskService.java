package com.mt.saga.domain.model.task.create_order_task;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.domain_event.DomainEventPublisher;
import com.mt.common.domain.model.domain_event.SubscribeForEvent;
import com.mt.saga.domain.DomainRegistry;
import com.mt.saga.domain.model.order_state_machine.event.UserPlaceOrderEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse.CancelPaymentQRLinkEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse.DeleteNewOrderEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse.IncreaseOrderStorageEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse.RestoreCartEvent;
import com.mt.saga.domain.model.order_state_machine.order.CartDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Service
public class CreateOrderTaskService {
    @Transactional
    @SubscribeForEvent
    public void rollbackCreate(CreateOrderTask bizTx) {
        UserPlaceOrderEvent command = CommonDomainRegistry.getCustomObjectSerializer().deserialize(bizTx.getCreateBizStateMachineCommand(), UserPlaceOrderEvent.class);
        log.info("start of cancel task of {} with {}", bizTx.getForwardChangeId(), bizTx.getReverseChangeId());
        DomainEventPublisher.instance().publish(new CancelPaymentQRLinkEvent(bizTx.getOrderId(), bizTx.getReverseChangeId(), bizTx.getId(), bizTx.getForwardChangeId()));
        DomainEventPublisher.instance().publish(new IncreaseOrderStorageEvent(DomainRegistry.getProductService().getReserveOrderPatchCommands(command.getProductList()), bizTx.getReverseChangeId(), bizTx.getId()));
        Set<String> collect = command.getProductList().stream().map(CartDetail::getCartId).collect(Collectors.toSet());
        DomainEventPublisher.instance().publish(new RestoreCartEvent(command.getUserId(), collect, bizTx.getReverseChangeId(), bizTx.getId(), bizTx.getForwardChangeId()));
        DomainEventPublisher.instance().publish(new DeleteNewOrderEvent(command.getOrderId(), command.getUserId(), bizTx.getId(), bizTx.getReverseChangeId(), bizTx.getForwardChangeId()));
        bizTx.setAcknowledged(true);
        DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(bizTx);
    }
}

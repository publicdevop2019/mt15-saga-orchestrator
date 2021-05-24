package com.mt.saga.port.adapter.messaging;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.saga.appliction.ApplicationServiceRegistry;
import com.mt.saga.domain.model.order_state_machine.event.UserPlaceOrderEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DomainEventSubscriber {
    private static final String SAGA_ORDER_QUEUE_NAME = "order_operation_event_saga_handler";
    private static final String TASK_UPDATE_QUEUE_NAME2 = "clear_cart_reply_event_handler";
    private static final String TASK_UPDATE_QUEUE_NAME3 = "create_new_order_reply_event_handler";
    private static final String TASK_UPDATE_QUEUE_NAME4 = "decrease_sku_for_order_reply_event_handler";
    private static final String TASK_UPDATE_QUEUE_NAME6 = "generate_order_payment_link_reply_event_handler";
    @Value("${mt.app.name.mt2}")
    private String profileAppName;
    @Value("${mt.app.name.mt3}")
    private String mallAppName;
    @Value("${mt.app.name.mt6}")
    private String paymentAppName;

    @EventListener(ApplicationReadyEvent.class)
    private void listener() {
        CommonDomainRegistry.getEventStreamService().subscribe(profileAppName, false, SAGA_ORDER_QUEUE_NAME, (event) -> {
            log.debug("handling order_operation_event with id {}", event.getId());
            UserPlaceOrderEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), UserPlaceOrderEvent.class);
            ApplicationServiceRegistry.getStateMachineApplicationService().start(deserialize);
        }, "order_operation_event");
    }

    @EventListener(ApplicationReadyEvent.class)
    private void listener2() {
        CommonDomainRegistry.getEventStreamService().subscribe(profileAppName, false, TASK_UPDATE_QUEUE_NAME2, (event) -> {
            log.debug("handling clear_cart_reply_event with id {}", event.getId());
            ClearCartResultEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), ClearCartResultEvent.class);
            ApplicationServiceRegistry.getTaskApplicationService().updateCreateNewOrderTask(deserialize);
        }, "clear_cart_reply_event");
    }

    @EventListener(ApplicationReadyEvent.class)
    private void listener3() {
        CommonDomainRegistry.getEventStreamService().subscribe(profileAppName, false, TASK_UPDATE_QUEUE_NAME3, (event) -> {
            log.debug("handling create_new_order_reply_event with id {}", event.getId());
            CreateNewOrderReplyEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), CreateNewOrderReplyEvent.class);
            ApplicationServiceRegistry.getTaskApplicationService().updateCreateNewOrderTask(deserialize);
        }, "create_new_order_reply_event");
    }

    @EventListener(ApplicationReadyEvent.class)
    private void listener4() {
        CommonDomainRegistry.getEventStreamService().subscribe(mallAppName, false, TASK_UPDATE_QUEUE_NAME4, (event) -> {
            log.debug("handling decrease_sku_for_order_reply_event with id {}", event.getId());
            DecreaseOrderStorageResultEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), DecreaseOrderStorageResultEvent.class);
            ApplicationServiceRegistry.getTaskApplicationService().updateCreateNewOrderTask(deserialize);
        }, "decrease_sku_for_order_reply_event");
    }

    @EventListener(ApplicationReadyEvent.class)
    private void listener6() {
        CommonDomainRegistry.getEventStreamService().subscribe(paymentAppName, false, TASK_UPDATE_QUEUE_NAME6, (event) -> {
            log.debug("handling generate_order_payment_link_reply_event with id {}", event.getId());
            GeneratePaymentQRLinkResultEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), GeneratePaymentQRLinkResultEvent.class);
            ApplicationServiceRegistry.getTaskApplicationService().updateCreateNewOrderTask(deserialize);
        }, "generate_order_payment_link_reply_event");
    }

}

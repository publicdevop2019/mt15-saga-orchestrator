package com.mt.saga.port.adapter.messaging;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.saga.appliction.ApplicationServiceRegistry;
import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent.TOPIC_ORDER_COMMAND;
import static com.mt.saga.domain.model.order_state_machine.event.create_new_order.ClearCartResultEvent.CREATE_NEW_ORDER_REPLY;

@Slf4j
@Component
public class DomainEventSubscriber {
    private static final String SAGA_ORDER_QUEUE_NAME = "saga_order_queue";
    private static final String TASK_UPDATE_QUEUE_NAME = "task_update_queue";
    @Value("${mt.app.name.mt2}")
    private String profileAppName;
    @Value("${mt.app.name.mt3}")
    private String mallAppName;
    @Value("${mt.app.name.mt5}")
    private String paymentAppName;

    @EventListener(ApplicationReadyEvent.class)
    private void listener() {
        CommonDomainRegistry.getEventStreamService().subscribe(profileAppName, false, SAGA_ORDER_QUEUE_NAME, (event) -> {
            if ("OrderOperationEvent".equals(event.getName())) {
                log.debug("handling event with id {}", event.getId());
                OrderOperationEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), OrderOperationEvent.class);
                ApplicationServiceRegistry.getStateMachineApplicationService().start(deserialize);
            }
        }, TOPIC_ORDER_COMMAND);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void listener2() {
        CommonDomainRegistry.getEventStreamService().subscribe(profileAppName, false, TASK_UPDATE_QUEUE_NAME, (event) -> {
            if (ClearCartResultEvent.class.getName().equals(event.getName())) {
                log.debug("handling event with id {}", event.getId());
                ClearCartResultEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), ClearCartResultEvent.class);
                ApplicationServiceRegistry.getTaskApplicationService().updateCreateNewOrderTask(deserialize);
            }
            if (CreateNewOrderResultEvent.class.getName().equals(event.getName())) {
                log.debug("handling event with id {}", event.getId());
                CreateNewOrderResultEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), CreateNewOrderResultEvent.class);
                ApplicationServiceRegistry.getTaskApplicationService().updateCreateNewOrderTask(deserialize);
            }
        }, profileAppName);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void listener4() {
        CommonDomainRegistry.getEventStreamService().subscribe(mallAppName, false, TASK_UPDATE_QUEUE_NAME, (event) -> {
            if (DecreaseOrderStorageResultEvent.class.getName().equals(event.getName())) {
                log.debug("handling event with id {}", event.getId());
                DecreaseOrderStorageResultEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), DecreaseOrderStorageResultEvent.class);
                ApplicationServiceRegistry.getTaskApplicationService().updateCreateNewOrderTask(deserialize);
            }
            if (ValidateProductResultEvent.class.getName().equals(event.getName())) {
                log.debug("handling event with id {}", event.getId());
                ValidateProductResultEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), ValidateProductResultEvent.class);
                ApplicationServiceRegistry.getTaskApplicationService().updateCreateNewOrderTask(deserialize);
            }
        }, CREATE_NEW_ORDER_REPLY);
    }
    @EventListener(ApplicationReadyEvent.class)
    private void listener5() {
        CommonDomainRegistry.getEventStreamService().subscribe(paymentAppName, false, TASK_UPDATE_QUEUE_NAME, (event) -> {
            if (GeneratePaymentQRLinkResultEvent.class.getName().equals(event.getName())) {
                log.debug("handling event with id {}", event.getId());
                GeneratePaymentQRLinkResultEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), GeneratePaymentQRLinkResultEvent.class);
                ApplicationServiceRegistry.getTaskApplicationService().updateCreateNewOrderTask(deserialize);
            }
        }, CREATE_NEW_ORDER_REPLY);
    }

}

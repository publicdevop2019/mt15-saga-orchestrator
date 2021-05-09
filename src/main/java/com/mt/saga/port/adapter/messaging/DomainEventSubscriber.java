package com.mt.saga.port.adapter.messaging;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.restful.exception.AggregateOutdatedException;
import com.mt.common.domain.model.sql.builder.UpdateQueryBuilder;
import com.mt.saga.appliction.ApplicationServiceRegistry;
import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent.TOPIC_ORDER_COMMAND;

@Slf4j
@Component
public class DomainEventSubscriber {
    private static final String SAGA_ORDER_QUEUE_NAME = "saga_order_queue";
    @Value("${spring.application.name}")
    private String appName;

    @EventListener(ApplicationReadyEvent.class)
    private void listener() {
        CommonDomainRegistry.getEventStreamService().subscribe(appName, true, SAGA_ORDER_QUEUE_NAME, (event) -> {
            try {
                if (OrderOperationEvent.class.getName().equals(event.getName())) {
                    log.debug("handling event with id {}", event.getId());
                    OrderOperationEvent deserialize = CommonDomainRegistry.getCustomObjectSerializer().deserialize(event.getEventBody(), OrderOperationEvent.class);
                    ApplicationServiceRegistry.getStateMachineApplicationService().start(deserialize);
                }
            } catch (UpdateQueryBuilder.PatchCommandExpectNotMatchException | AggregateOutdatedException ex) {
                //ignore above ex
                log.debug("ignore exception in event {}", ex.getClass().toString());
            }
        }, TOPIC_ORDER_COMMAND);
    }

}

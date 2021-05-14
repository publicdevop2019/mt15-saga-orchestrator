package com.mt.saga.appliction.task;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.domain_event.DomainEventPublisher;
import com.mt.common.domain.model.domain_event.SubscribeForEvent;
import com.mt.saga.domain.DomainRegistry;
import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.*;
import com.mt.saga.domain.model.order_state_machine.order.AppCreateBizOrderCommand;
import com.mt.saga.domain.model.task.SubTaskStatus;
import com.mt.saga.domain.model.task.conclude_order_task.ConcludeOrderTask;
import com.mt.saga.domain.model.task.confirm_order_payment_task.ConfirmOrderPaymentTask;
import com.mt.saga.domain.model.task.create_order_task.CreateOrderTask;
import com.mt.saga.domain.model.task.recycle_order_task.RecycleOrderTask;
import com.mt.saga.domain.model.task.reserve_order_task.ReserveOrderTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class TaskApplicationService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreateOrderTask createCreateOrderTask(OrderOperationEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        CreateOrderTask createOrderTask = new CreateOrderTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(createOrderTask);
        return createOrderTask;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecycleOrderTask createRecycleOrderTask(OrderOperationEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        RecycleOrderTask recycleOrderTask = new RecycleOrderTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getRecycleOrderTaskRepository().add(recycleOrderTask);
        return recycleOrderTask;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReserveOrderTask createReserveOrderTask(OrderOperationEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        ReserveOrderTask task = new ReserveOrderTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getReserveOrderTaskRepository().add(task);
        return task;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmOrderPaymentTask createConfirmOrderPaymentTask(OrderOperationEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        ConfirmOrderPaymentTask task = new ConfirmOrderPaymentTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getConfirmOrderPaymentTaskRepository().add(task);
        return task;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConcludeOrderTask createConcludeOrderTask(OrderOperationEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        ConcludeOrderTask task = new ConcludeOrderTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getConcludeOrderTaskRepository().add(task);
        return task;
    }

    @Transactional

    public void updateCreateNewOrderTask(ClearCartResultEvent deserialize) {
        Optional<CreateOrderTask> byIdLocked = DomainRegistry.getCreateOrderTaskRepository().findByIdLocked(deserialize.getTaskId());
        byIdLocked.ifPresent(e -> {
            if (deserialize.isSuccess()) {
                e.setRemoveItemsFromCartSubTaskStatus(SubTaskStatus.COMPLETED);
            } else {
                e.setRemoveItemsFromCartSubTaskStatus(SubTaskStatus.FAILED);
            }
            e.checkAllSubTaskStatus();
            DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(e);
        });
    }

    @Transactional
    public void updateCreateNewOrderTask(DecreaseOrderStorageResultEvent deserialize) {
        Optional<CreateOrderTask> byIdLocked = DomainRegistry.getCreateOrderTaskRepository().findByIdLocked(deserialize.getTaskId());
        byIdLocked.ifPresent(e -> {
            if (deserialize.isSuccess()) {
                e.setDecreaseOrderStorageSubTaskStatus(SubTaskStatus.COMPLETED);
            } else {
                e.setDecreaseOrderStorageSubTaskStatus(SubTaskStatus.FAILED);
            }
            e.checkAllSubTaskStatus();
            DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(e);
        });
    }

    @Transactional
    @SubscribeForEvent
    public void updateCreateNewOrderTask(GeneratePaymentQRLinkResultEvent deserialize) {
        Optional<CreateOrderTask> byIdLocked = DomainRegistry.getCreateOrderTaskRepository().findByIdLocked(deserialize.getTaskId());
        byIdLocked.ifPresent(e -> {
            if (deserialize.isSuccess()) {
                e.getGeneratePaymentLinkSubTask().setStatus(SubTaskStatus.COMPLETED);
                String createBizStateMachineCommand = e.getCreateBizStateMachineCommand();
                OrderOperationEvent deserialize1 = CommonDomainRegistry.getCustomObjectSerializer().deserialize(createBizStateMachineCommand, OrderOperationEvent.class);
                AppCreateBizOrderCommand appCreateBizOrderCommand = DomainRegistry.getOrderService().getAppCreateBizOrderCommand(deserialize1, deserialize.getPaymentLink());
                DomainEventPublisher.instance().publish(new CreateNewOrderEvent(appCreateBizOrderCommand, deserialize1.getTxId()));
                e.getGeneratePaymentLinkSubTask().setResults(deserialize.getPaymentLink());
            } else {
                e.getGeneratePaymentLinkSubTask().setStatus(SubTaskStatus.FAILED);
            }
            e.checkAllSubTaskStatus();
            DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(e);
        });
    }

    @Transactional
    public void updateCreateNewOrderTask(CreateNewOrderResultEvent deserialize) {
        Optional<CreateOrderTask> byIdLocked = DomainRegistry.getCreateOrderTaskRepository().findByIdLocked(deserialize.getTaskId());
        byIdLocked.ifPresent(e -> {
            if (deserialize.isSuccess()) {
                e.getCreateOrderSubTask().setStatus(SubTaskStatus.COMPLETED);
                e.getCreateOrderSubTask().setResult(true);
            } else {
                e.getCreateOrderSubTask().setStatus(SubTaskStatus.FAILED);
                e.getCreateOrderSubTask().setResult(false);
            }
            e.checkAllSubTaskStatus();
            DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(e);
        });
    }
}

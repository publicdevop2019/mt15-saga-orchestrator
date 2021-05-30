package com.mt.saga.appliction.task;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.common.domain.model.domain_event.DomainEventPublisher;
import com.mt.common.domain.model.domain_event.SubscribeForEvent;
import com.mt.saga.domain.DomainRegistry;
import com.mt.saga.domain.model.order_state_machine.event.UserPlaceOrderEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.forward.*;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.reverse.CancelPaymentQRLinkReplyEvent;
import com.mt.saga.domain.model.task.SubTaskStatus;
import com.mt.saga.domain.model.task.conclude_order_task.ConcludeOrderTask;
import com.mt.saga.domain.model.task.confirm_order_payment_task.ConfirmOrderPaymentTask;
import com.mt.saga.domain.model.task.create_order_task.CreateOrderTask;
import com.mt.saga.domain.model.task.recycle_order_task.RecycleOrderTask;
import com.mt.saga.domain.model.task.reserve_order_task.ReserveOrderTask;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TaskApplicationService {
    @Autowired
    private RedissonClient redissonClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreateOrderTask createCreateOrderTask(UserPlaceOrderEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        CreateOrderTask createOrderTask = new CreateOrderTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(createOrderTask);
        return createOrderTask;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecycleOrderTask createRecycleOrderTask(UserPlaceOrderEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        RecycleOrderTask recycleOrderTask = new RecycleOrderTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getRecycleOrderTaskRepository().add(recycleOrderTask);
        return recycleOrderTask;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReserveOrderTask createReserveOrderTask(UserPlaceOrderEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        ReserveOrderTask task = new ReserveOrderTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getReserveOrderTaskRepository().add(task);
        return task;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmOrderPaymentTask createConfirmOrderPaymentTask(UserPlaceOrderEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        ConfirmOrderPaymentTask task = new ConfirmOrderPaymentTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getConfirmOrderPaymentTaskRepository().add(task);
        return task;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConcludeOrderTask createConcludeOrderTask(UserPlaceOrderEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        ConcludeOrderTask task = new ConcludeOrderTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getConcludeOrderTaskRepository().add(task);
        return task;
    }

    @Transactional
    public void updateCreateNewOrderTask(ClearCartReplyEvent deserialize) {
        log.debug("before updating task with id {}, acquire lock", deserialize.getTaskId());
        RLock lock = redissonClient.getLock(deserialize.getTaskId() + "_task");
        lock.lock(5, TimeUnit.SECONDS);
        log.debug("lock acquired");
        Optional<CreateOrderTask> byIdLocked = DomainRegistry.getCreateOrderTaskRepository().getById(deserialize.getTaskId());
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
    public void updateCreateNewOrderTask(DecreaseOrderStorageReplyEvent deserialize) {
        log.debug("before updating task with id {}, acquire lock", deserialize.getTaskId());
        RLock lock = redissonClient.getLock(deserialize.getTaskId() + "_task");
        lock.lock(5, TimeUnit.SECONDS);
        log.debug("lock acquired");
        Optional<CreateOrderTask> byIdLocked = DomainRegistry.getCreateOrderTaskRepository().getById(deserialize.getTaskId());
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
    public void updateCreateNewOrderTask(GeneratePaymentQRLinkReplyEvent deserialize) {
        log.debug("before updating task with id {}, acquire lock", deserialize.getTaskId());
        RLock lock = redissonClient.getLock(deserialize.getTaskId() + "_task");
        lock.lock(5, TimeUnit.SECONDS);
        log.debug("lock acquired");
        Optional<CreateOrderTask> byIdLocked = DomainRegistry.getCreateOrderTaskRepository().getById(deserialize.getTaskId());
        byIdLocked.ifPresent(e -> {
            if (deserialize.getPaymentLink() != null && !deserialize.getPaymentLink().isBlank()) {
                e.getGeneratePaymentLinkSubTask().setResults(deserialize.getPaymentLink());
                e.getGeneratePaymentLinkSubTask().setStatus(SubTaskStatus.COMPLETED);
                String createBizStateMachineCommand = e.getCreateBizStateMachineCommand();
                UserPlaceOrderEvent placeOrderEvent = CommonDomainRegistry.getCustomObjectSerializer().deserialize(createBizStateMachineCommand, UserPlaceOrderEvent.class);
                CreateNewOrderEvent event = new CreateNewOrderEvent(placeOrderEvent, deserialize.getPaymentLink(), e.getId(), e.getForwardChangeId());
                DomainEventPublisher.instance().publish(event);
            } else {
                e.getGeneratePaymentLinkSubTask().setStatus(SubTaskStatus.FAILED);
            }
            e.checkAllSubTaskStatus();
            DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(e);
        });
    }

    @Transactional
    public void updateCreateNewOrderTask(CreateNewOrderReplyEvent deserialize) {
        log.debug("before updating task with id {}, acquire lock", deserialize.getTaskId());
        RLock lock = redissonClient.getLock(deserialize.getTaskId() + "_task");
        lock.lock(5, TimeUnit.SECONDS);
        log.debug("lock acquired");
        Optional<CreateOrderTask> byIdLocked = DomainRegistry.getCreateOrderTaskRepository().getById(deserialize.getTaskId());
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

    @Transactional
    public void updateCreateNewOrderTask(CancelPaymentQRLinkReplyEvent deserialize) {
        log.debug("before updating task with id {}, acquire lock", deserialize.getTaskId());
        RLock lock = redissonClient.getLock(deserialize.getTaskId() + "_task");
        lock.lock(5, TimeUnit.SECONDS);
        log.debug("lock acquired");
        Optional<CreateOrderTask> byIdLocked = DomainRegistry.getCreateOrderTaskRepository().getById(deserialize.getTaskId());
        byIdLocked.ifPresent(e -> {
            if (deserialize.isSuccess()) {
                e.getCreateOrderSubTask().setStatus(SubTaskStatus.CANCELLED);
            } else {
                e.getCreateOrderSubTask().setStatus(SubTaskStatus.FAILED);
            }
            e.checkAllSubTaskStatus();
            DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(e);
        });
    }
}

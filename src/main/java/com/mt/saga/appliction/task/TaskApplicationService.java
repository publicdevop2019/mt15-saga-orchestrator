package com.mt.saga.appliction.task;

import com.mt.common.domain.CommonDomainRegistry;
import com.mt.saga.domain.DomainRegistry;
import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;
import com.mt.saga.domain.model.task.conclude_order_task.ConcludeOrderTask;
import com.mt.saga.domain.model.task.confirm_order_payment_task.ConfirmOrderPaymentTask;
import com.mt.saga.domain.model.task.create_order_task.CreateOrderTask;
import com.mt.saga.domain.model.task.recycle_order_task.RecycleOrderTask;
import com.mt.saga.domain.model.task.reserve_order_task.ReserveOrderTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TaskApplicationService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreateOrderTask createCreateOrderTask(OrderOperationEvent customerOrder) {
        String serialize = CommonDomainRegistry.getCustomObjectSerializer().serialize(customerOrder);
        CreateOrderTask createOrderTask = new CreateOrderTask(serialize, customerOrder.getTxId(), customerOrder.getOrderId());
        DomainRegistry.getCreateOrderTaskRepository().add(createOrderTask);
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
}

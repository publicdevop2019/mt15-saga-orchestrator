package com.mt.saga.domain;

import com.mt.saga.domain.model.order_state_machine.OrderStateMachineBuilder;
import com.mt.saga.domain.model.task.conclude_order_task.ConcludeOrderTaskRepository;
import com.mt.saga.domain.model.task.confirm_order_payment_task.ConfirmOrderPaymentTaskRepository;
import com.mt.saga.domain.model.task.create_order_task.CreateOrderTaskRepository;
import com.mt.saga.domain.model.task.recycle_order_task.RecycleOrderTaskRepository;
import com.mt.saga.domain.model.task.reserve_order_task.ReserveOrderTaskRepository;
import com.mt.saga.port.adapter.persistence.task.SpringDataJpaCreateOrderTaskRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DomainRegistry {
    @Getter
    private static OrderStateMachineBuilder orderStateMachineBuilder;
    @Getter
    private static CreateOrderTaskRepository createOrderTaskRepository;
    @Getter
    private static RecycleOrderTaskRepository recycleOrderTaskRepository;
    @Getter
    private static ReserveOrderTaskRepository reserveOrderTaskRepository;
    @Getter
    private static ConfirmOrderPaymentTaskRepository confirmOrderPaymentTaskRepository;
    @Getter
    private static ConcludeOrderTaskRepository concludeOrderTaskRepository;

    @Autowired
    private void setConcludeOrderTaskRepository(ConcludeOrderTaskRepository concludeOrderTaskRepository) {
        DomainRegistry.concludeOrderTaskRepository = concludeOrderTaskRepository;
    }

    @Autowired
    private void setReserveOrderTaskRepository(ReserveOrderTaskRepository reserveOrderTaskRepository) {
        DomainRegistry.reserveOrderTaskRepository = reserveOrderTaskRepository;
    }

    @Autowired
    private void setConfirmOrderPaymentTaskRepository(ConfirmOrderPaymentTaskRepository confirmOrderPaymentTaskRepository) {
        DomainRegistry.confirmOrderPaymentTaskRepository = confirmOrderPaymentTaskRepository;
    }

    @Autowired
    private void setRecycleOrderTaskRepository(RecycleOrderTaskRepository recycleOrderTaskRepository) {
        DomainRegistry.recycleOrderTaskRepository = recycleOrderTaskRepository;
    }

    @Autowired
    private void setOrderStateMachineBuilder(OrderStateMachineBuilder orderStateMachineBuilder) {
        DomainRegistry.orderStateMachineBuilder = orderStateMachineBuilder;
    }

    @Autowired
    private void setCreateOrderTaskRepository(SpringDataJpaCreateOrderTaskRepository createOrderTaskRepository) {
        DomainRegistry.createOrderTaskRepository = createOrderTaskRepository;
    }
}

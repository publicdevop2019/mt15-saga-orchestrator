package com.mt.saga.domain.model.task.create_order_task;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface CreateOrderTaskRepository {
    List<CreateOrderTask> findRollbackTasks(Date from);

    void add(CreateOrderTask createOrderTask);

    Optional<CreateOrderTask> findByIdLocked(Long id);
}
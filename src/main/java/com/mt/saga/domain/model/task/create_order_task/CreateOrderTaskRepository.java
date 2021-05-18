package com.mt.saga.domain.model.task.create_order_task;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface CreateOrderTaskRepository {
    List<CreateOrderTask> findRollbackTasks(Date from);

    void createOrUpdate(CreateOrderTask createOrderTask);

    Optional<CreateOrderTask> getById(Long id);
}

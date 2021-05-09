package com.mt.saga.domain.model.task.reserve_order_task;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ReserveOrderTaskRepository {
    List<ReserveOrderTask> findRollbackTasks(Date from);

    void add(ReserveOrderTask task);
}

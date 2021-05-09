package com.mt.saga.domain.model.task.recycle_order_task;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface RecycleOrderTaskRepository {

    List<RecycleOrderTask> findRollbackTasks(Date from);

    void add(RecycleOrderTask createOrderTask);
}

package com.mt.saga.domain.model.task.recycle_order_task;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecycleOrderTaskRepository {

    List<RecycleOrderTask> findRollbackTasks(Date from);

    void add(RecycleOrderTask createOrderTask);

    Optional<RecycleOrderTask> findByIdLocked(Long id);
}

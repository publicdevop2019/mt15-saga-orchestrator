package com.mt.saga.domain.model.task.conclude_order_task;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConcludeOrderTaskRepository {

    List<ConcludeOrderTask> findRollbackTasks(Date from);

    void add(ConcludeOrderTask createOrderTask);

    Optional<ConcludeOrderTask> findByIdLocked(Long id);
}

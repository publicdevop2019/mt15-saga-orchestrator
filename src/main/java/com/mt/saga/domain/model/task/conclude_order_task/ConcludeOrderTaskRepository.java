package com.mt.saga.domain.model.task.conclude_order_task;

import com.mt.saga.domain.model.task.create_order_task.CreateOrderTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConcludeOrderTaskRepository {

    List<ConcludeOrderTask> findRollbackTasks(Date from);
    void add(ConcludeOrderTask createOrderTask);

}

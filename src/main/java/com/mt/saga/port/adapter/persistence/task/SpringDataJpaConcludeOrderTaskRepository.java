package com.mt.saga.port.adapter.persistence.task;

import com.mt.saga.domain.model.task.conclude_order_task.ConcludeOrderTask;
import com.mt.saga.domain.model.task.conclude_order_task.ConcludeOrderTaskRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataJpaConcludeOrderTaskRepository extends JpaRepository<ConcludeOrderTask, Long>, ConcludeOrderTaskRepository {
    @Query("SELECT p FROM #{#entityName} as p WHERE p.createdAt < ?1 AND (p.taskStatus = 'STARTED' OR p.taskStatus = 'FAILED') AND p.cancelBlocked = false")
    List<ConcludeOrderTask> findExpiredStartedOrFailNonBlockedTxs(Date from);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT p FROM #{#entityName} as p WHERE p.id = ?1")
    Optional<ConcludeOrderTask> findByIdOptLock(Long id);

    @Override
    default void add(ConcludeOrderTask createOrderTask) {
        save(createOrderTask);
    }

    @Override
    default List<ConcludeOrderTask> findRollbackTasks(Date from) {
        return findExpiredStartedOrFailNonBlockedTxs(from);
    }
}

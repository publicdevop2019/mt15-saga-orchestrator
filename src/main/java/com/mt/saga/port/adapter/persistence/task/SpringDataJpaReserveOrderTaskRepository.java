package com.mt.saga.port.adapter.persistence.task;

import com.mt.saga.domain.model.task.reserve_order_task.ReserveOrderTask;
import com.mt.saga.domain.model.task.reserve_order_task.ReserveOrderTaskRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataJpaReserveOrderTaskRepository extends JpaRepository<ReserveOrderTask, Long>, ReserveOrderTaskRepository {
    @Query("SELECT p FROM #{#entityName} as p WHERE p.createdAt < ?1 AND (p.taskStatus = 'STARTED' OR p.taskStatus = 'FAILED') AND p.cancelBlocked = false")
    List<ReserveOrderTask> findExpiredStartedOrFailNonBlockedTxs(Date from);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT p FROM #{#entityName} as p WHERE p.id = ?1")
    Optional<ReserveOrderTask> findByIdOptLock(Long id);

    @Override
    default void add(ReserveOrderTask createOrderTask) {
        save(createOrderTask);
    }

    @Override
    default Optional<ReserveOrderTask> findByIdLocked(Long id) {
        return findByIdOptLock(id);
    }

    @Override
    default List<ReserveOrderTask> findRollbackTasks(Date from) {
        return findExpiredStartedOrFailNonBlockedTxs(from);
    }
}

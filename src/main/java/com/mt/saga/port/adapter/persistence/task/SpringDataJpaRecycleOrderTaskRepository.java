package com.mt.saga.port.adapter.persistence.task;

import com.mt.saga.domain.model.task.recycle_order_task.RecycleOrderTask;
import com.mt.saga.domain.model.task.recycle_order_task.RecycleOrderTaskRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataJpaRecycleOrderTaskRepository extends JpaRepository<RecycleOrderTask, Long>, RecycleOrderTaskRepository {
    @Query("SELECT p FROM #{#entityName} as p WHERE p.createdAt < ?1 AND (p.taskStatus = 'STARTED' OR p.taskStatus = 'FAILED') AND p.cancelBlocked = false")
    List<RecycleOrderTask> findExpiredStartedOrFailNonBlockedTxs(Date from);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT p FROM #{#entityName} as p WHERE p.id = ?1")
    Optional<RecycleOrderTask> findByIdOptLock(Long id);

    @Override
    default void add(RecycleOrderTask createOrderTask) {
        save(createOrderTask);
    }

    @Override
    default List<RecycleOrderTask> findRollbackTasks(Date from) {
        return findExpiredStartedOrFailNonBlockedTxs(from);
    }
}

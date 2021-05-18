package com.mt.saga.port.adapter.persistence.task;

import com.mt.saga.domain.model.task.create_order_task.CreateOrderTask;
import com.mt.saga.domain.model.task.create_order_task.CreateOrderTaskRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataJpaCreateOrderTaskRepository extends JpaRepository<CreateOrderTask, Long>, CreateOrderTaskRepository {
    @Query("SELECT p FROM #{#entityName} as p WHERE p.createdAt < ?1 AND (p.taskStatus = 'STARTED' OR p.taskStatus = 'FAILED') AND p.cancelBlocked = false")
    List<CreateOrderTask> findExpiredStartedOrFailNonBlockedTxs(Date from);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT p FROM #{#entityName} as p WHERE p.id = ?1")
    Optional<CreateOrderTask> findByIdOptLock(Long id);

    @Override

    default void createOrUpdate(CreateOrderTask createOrderTask) {
        save(createOrderTask);
    }

    @Override
    default Optional<CreateOrderTask> getById(Long id) {
        return findById(id);
    }

    @Override
    default List<CreateOrderTask> findRollbackTasks(Date from) {
        return findExpiredStartedOrFailNonBlockedTxs(from);
    }
}

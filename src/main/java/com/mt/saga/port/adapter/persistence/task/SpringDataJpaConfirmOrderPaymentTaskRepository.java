package com.mt.saga.port.adapter.persistence.task;

import com.mt.saga.domain.model.task.confirm_order_payment_task.ConfirmOrderPaymentTask;
import com.mt.saga.domain.model.task.confirm_order_payment_task.ConfirmOrderPaymentTaskRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataJpaConfirmOrderPaymentTaskRepository extends JpaRepository<ConfirmOrderPaymentTask, Long>, ConfirmOrderPaymentTaskRepository {
    @Query("SELECT p FROM #{#entityName} as p WHERE p.createdAt < ?1 AND (p.taskStatus = 'STARTED' OR p.taskStatus = 'FAILED') AND p.cancelBlocked = false")
    List<ConfirmOrderPaymentTask> findExpiredStartedOrFailNonBlockedTxs(Date from);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT p FROM #{#entityName} as p WHERE p.id = ?1")
    Optional<ConfirmOrderPaymentTask> findByIdOptLock(Long id);

    @Override
    default void add(ConfirmOrderPaymentTask createOrderTask) {
        save(createOrderTask);
    }

    @Override
    default List<ConfirmOrderPaymentTask> findRollbackTasks(Date from) {
        return findExpiredStartedOrFailNonBlockedTxs(from);
    }
}

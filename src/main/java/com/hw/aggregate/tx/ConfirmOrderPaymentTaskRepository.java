package com.hw.aggregate.tx;

import com.hw.aggregate.tx.model.ConfirmOrderPaymentTask;
import com.hw.aggregate.tx.model.CreateOrderTask;
import com.hw.aggregate.tx.model.RecycleOrderTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConfirmOrderPaymentTaskRepository extends JpaRepository<ConfirmOrderPaymentTask, Long> {
    @Query("SELECT p FROM #{#entityName} as p WHERE p.createdAt < ?1 AND (p.taskStatus = 'STARTED' OR p.taskStatus = 'FAILED') AND p.cancelBlocked = false")
    List<ConfirmOrderPaymentTask> findExpiredStartedOrFailNonBlockedTxs(Date from);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT p FROM #{#entityName} as p WHERE p.id = ?1")
    Optional<ConfirmOrderPaymentTask> findByIdOptLock(Long id);

}

package com.hw.aggregate.tx;

import com.hw.aggregate.tx.model.CreateOrderBizTx;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface BizTxRepository extends JpaRepository<CreateOrderBizTx, Long> {
    @Query("SELECT p FROM #{#entityName} as p WHERE p.createdAt < ?1 AND (p.txStatus = 'STARTED' OR p.txStatus = 'FAILED')")
    List<CreateOrderBizTx> findExpiredStartedOrFailTxs(Date from);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT p FROM #{#entityName} as p WHERE p.id = ?1")
    Optional<CreateOrderBizTx> findByIdOptLock(Long id);

}

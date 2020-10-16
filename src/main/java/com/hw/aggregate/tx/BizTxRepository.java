package com.hw.aggregate.tx;

import com.hw.aggregate.tx.model.BizTx;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface BizTxRepository extends JpaRepository<BizTx, Long> {
    @Query("SELECT p FROM #{#entityName} as p WHERE p.createdAt < ?1 AND (p.txStatus = 'STARTED' OR p.txStatus = 'FAIL')")
    List<BizTx> findExpiredStartedOrFailTxs(Date from);

//    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT p FROM #{#entityName} as p WHERE p.id = ?1")
    Optional<BizTx> findByIdOptLock(Long id);

}

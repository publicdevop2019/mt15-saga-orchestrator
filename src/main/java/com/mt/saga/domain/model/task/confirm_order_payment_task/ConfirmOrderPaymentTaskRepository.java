package com.mt.saga.domain.model.task.confirm_order_payment_task;

import com.mt.saga.domain.model.task.reserve_order_task.ReserveOrderTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConfirmOrderPaymentTaskRepository {
    List<ConfirmOrderPaymentTask> findRollbackTasks(Date from);

    void add(ConfirmOrderPaymentTask task);

}

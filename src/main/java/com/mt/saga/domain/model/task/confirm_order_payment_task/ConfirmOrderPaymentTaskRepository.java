package com.mt.saga.domain.model.task.confirm_order_payment_task;

import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConfirmOrderPaymentTaskRepository {
    List<ConfirmOrderPaymentTask> findRollbackTasks(Date from);

    void add(ConfirmOrderPaymentTask task);

    Optional<ConfirmOrderPaymentTask> findByIdLocked(Long id);
}

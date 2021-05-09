package com.mt.saga.domain.model.task;

public interface PaymentService {
    void cancelPaymentLink(String cancelTxId, String txId);

    String generatePaymentLink(String orderId, String changeId);

    Boolean confirmPaymentStatus(String orderId);

}

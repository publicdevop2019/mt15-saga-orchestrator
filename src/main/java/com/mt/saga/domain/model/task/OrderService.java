package com.mt.saga.domain.model.task;

import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;

public interface OrderService {
    void concludeOrder(OrderOperationEvent command);

    void reservedOrder(OrderOperationEvent command);

    void cancelCreateNewOrder(OrderOperationEvent command, String cancelTxId, String txId);

    void cancelConcludeOrder(OrderOperationEvent command, String cancelTxId, String txId);

    void cancelReserveOrder(OrderOperationEvent command, String cancelTxId, String txId);

    void cancelRecycleOrder(OrderOperationEvent command, String cancelTxId, String txId);

    void cancelConfirmPayment(OrderOperationEvent command, String cancelTxId, String txId);

    void createNewOrder(String paymentLink, OrderOperationEvent command);

    void confirmPayment(OrderOperationEvent command);

    void recycleOrder(OrderOperationEvent command);

}

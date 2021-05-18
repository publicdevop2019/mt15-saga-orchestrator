package com.mt.saga.domain.model.task;

import com.mt.saga.domain.model.order_state_machine.event.UserPlaceOrderEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.CreateNewOrderEvent;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderStatus;

public interface OrderService {
    void concludeOrder(UserPlaceOrderEvent command);

    void reservedOrder(UserPlaceOrderEvent command);

    void cancelCreateNewOrder(UserPlaceOrderEvent command, String cancelTxId, String txId);

    void cancelConcludeOrder(UserPlaceOrderEvent command, String cancelTxId, String txId);

    void cancelReserveOrder(UserPlaceOrderEvent command, String cancelTxId, String txId);

    void cancelRecycleOrder(UserPlaceOrderEvent command, String cancelTxId, String txId);

    void cancelConfirmPayment(UserPlaceOrderEvent command, String cancelTxId, String txId);

    void createNewOrder(CreateNewOrderEvent command, String changeId);

    void confirmPayment(UserPlaceOrderEvent command);

    void recycleOrder(UserPlaceOrderEvent command);
}

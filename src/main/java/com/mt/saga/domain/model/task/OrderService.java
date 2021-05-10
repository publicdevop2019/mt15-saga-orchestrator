package com.mt.saga.domain.model.task;

import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;
import com.mt.saga.domain.model.order_state_machine.order.AppCreateBizOrderCommand;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderStatus;

public interface OrderService {
    void concludeOrder(OrderOperationEvent command);

    void reservedOrder(OrderOperationEvent command);

    void cancelCreateNewOrder(OrderOperationEvent command, String cancelTxId, String txId);

    void cancelConcludeOrder(OrderOperationEvent command, String cancelTxId, String txId);

    void cancelReserveOrder(OrderOperationEvent command, String cancelTxId, String txId);

    void cancelRecycleOrder(OrderOperationEvent command, String cancelTxId, String txId);

    void cancelConfirmPayment(OrderOperationEvent command, String cancelTxId, String txId);

    void createNewOrder(AppCreateBizOrderCommand command, String changeId);

    void confirmPayment(OrderOperationEvent command);

    void recycleOrder(OrderOperationEvent command);

    default AppCreateBizOrderCommand getAppCreateBizOrderCommand(OrderOperationEvent command, String finalPaymentLink) {
        AppCreateBizOrderCommand appCreateBizOrderCommand = new AppCreateBizOrderCommand();
        appCreateBizOrderCommand.setAddress(command.getAddress());
        appCreateBizOrderCommand.setCreatedBy(command.getCreatedBy());
        appCreateBizOrderCommand.setOrderId(command.getOrderId());
        appCreateBizOrderCommand.setOrderState(BizOrderStatus.DRAFT);
        appCreateBizOrderCommand.setPaymentAmt(command.getPaymentAmt());
        appCreateBizOrderCommand.setPaymentType(command.getPaymentType());
        appCreateBizOrderCommand.setPaymentLink(finalPaymentLink);
        appCreateBizOrderCommand.setProductList(command.getProductList());
        appCreateBizOrderCommand.setUserId(command.getUserId());
        return appCreateBizOrderCommand;
    }
}

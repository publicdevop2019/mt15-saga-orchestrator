package com.mt.saga.domain.model.order_state_machine.event.create_new_order.forward;

import com.mt.common.domain.model.domain_event.DomainEvent;
import com.mt.saga.domain.model.order_state_machine.event.UserPlaceOrderEvent;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderAddressCmdRep;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderStatus;
import com.mt.saga.domain.model.order_state_machine.order.CartDetail;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateNewOrderEvent extends DomainEvent {
    private String orderId;
    private String userId;
    private BizOrderStatus orderState;
    private String createdBy;
    private BizOrderAddressCmdRep address;
    private List<CartDetail> productList;
    private String paymentType;
    private BigDecimal paymentAmt;
    private String paymentLink;
    private String changeId;
    private long taskId;

    public CreateNewOrderEvent(UserPlaceOrderEvent command, String finalPaymentLink,long taskId,String changeId) {
        setAddress(command.getAddress());
        setCreatedBy(command.getCreatedBy());
        setOrderId(command.getOrderId());
        setOrderState(BizOrderStatus.DRAFT);
        setPaymentAmt(command.getPaymentAmt());
        setPaymentType(command.getPaymentType());
        setPaymentLink(finalPaymentLink);
        setProductList(command.getProductList());
        setUserId(command.getUserId());
        setInternal(false);
        setTopic("create_new_order_event");
        setTaskId(taskId);
        setChangeId(changeId);
    }
}

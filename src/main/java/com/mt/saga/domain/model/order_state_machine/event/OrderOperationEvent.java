package com.mt.saga.domain.model.order_state_machine.event;

import com.mt.common.domain.model.domain_event.DomainEvent;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderAddressCmdRep;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderEvent;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderStatus;
import com.mt.saga.domain.model.order_state_machine.order.CartDetail;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderOperationEvent extends DomainEvent {
    public static final String TOPIC_ORDER_COMMAND = "ORDER_COMMAND";
    private String orderId;
    private String userId;
    private String txId;
    private BizOrderStatus orderState;
    private EventName eventName;
    private BizOrderEvent bizOrderEvent;
    private BizOrderEvent prepareEvent;
    private List<CartDetail> productList;
    private String createdBy;
    private BizOrderAddressCmdRep address;
    private String paymentType;
    private BigDecimal paymentAmt;
    private Integer version;

    public OrderOperationEvent() {
        setInternal(false);
        setTopic(TOPIC_ORDER_COMMAND);
    }

    public enum EventName {
        SUBMIT_TO_SAGA,
        CREATE_ORDER,
        UPDATE_ORDER,
        RESERVE_ORDER,
        RECYCLE_ORDER,
        CONCLUDE_ORDER,
        CONFIRM_PAYMENT,
        ;
    }
}

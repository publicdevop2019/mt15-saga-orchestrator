package com.mt.saga.domain.model.order_state_machine.order;

import lombok.Data;

import java.util.List;

@Data
public class AppValidateBizOrderCommand {
    private List<CartDetail> productList;
    private String orderId;
}

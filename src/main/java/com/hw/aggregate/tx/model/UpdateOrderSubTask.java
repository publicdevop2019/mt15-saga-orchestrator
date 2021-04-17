package com.hw.aggregate.tx.model;

import com.hw.aggregate.sm.model.order.CartDetail;

import java.util.List;

public class UpdateOrderSubTask {
    private SubTaskStatus status;
    private List<CartDetail> productList;
    private boolean result;
}

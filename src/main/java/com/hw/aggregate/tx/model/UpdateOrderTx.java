package com.hw.aggregate.tx.model;

import com.hw.aggregate.sm.model.order.CartDetail;

import java.util.List;

public class UpdateOrderTx {
    private SubTxStatus status;
    private List<CartDetail> productList;
    private boolean result;
}

package com.hw.aggregate.sm.model.order;

import lombok.Data;

import java.util.List;

@Data
public class AppValidateBizOrderCommand {
    private List<BizOrderItem> productList;
}

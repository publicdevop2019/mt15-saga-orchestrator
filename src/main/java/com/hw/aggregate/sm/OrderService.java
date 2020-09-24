package com.hw.aggregate.sm;

import com.hw.aggregate.sm.model.BizOrderStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public void validateProduct(long id) {

    }

    public void updateOrder(String paymentLink, BizOrderStatus status,boolean paymentStatus,String changeId) {

    }

    public void rollbackTransaction(String transactionId) {

    }
}

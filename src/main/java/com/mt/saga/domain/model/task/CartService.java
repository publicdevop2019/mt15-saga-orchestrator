package com.mt.saga.domain.model.task;

import java.util.Set;

public interface CartService {
    void cancelClearCart(String userId, Set<String> cartIds, String cancelTxId, String txId);

    void clearCart(String userId, Set<String> cartIds, String changeId);

}

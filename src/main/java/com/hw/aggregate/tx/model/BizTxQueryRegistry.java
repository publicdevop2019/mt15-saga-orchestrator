package com.hw.aggregate.tx.model;

import com.hw.shared.sql.RestfulQueryRegistry;
import org.springframework.stereotype.Component;

@Component
public class BizTxQueryRegistry extends RestfulQueryRegistry<CreateOrderTask> {

    @Override
    public Class<CreateOrderTask> getEntityClass() {
        return CreateOrderTask.class;
    }
}

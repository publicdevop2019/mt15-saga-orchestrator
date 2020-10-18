package com.hw.aggregate.tx.model;

import com.hw.shared.sql.RestfulQueryRegistry;
import org.springframework.stereotype.Component;

@Component
public class BizTxQueryRegistry extends RestfulQueryRegistry<BizTx> {

    @Override
    public Class<BizTx> getEntityClass() {
        return BizTx.class;
    }
}

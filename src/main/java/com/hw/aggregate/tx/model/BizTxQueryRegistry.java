package com.hw.aggregate.tx.model;

import com.hw.shared.sql.RestfulQueryRegistry;
import org.springframework.stereotype.Component;

@Component
public class BizTxQueryRegistry extends RestfulQueryRegistry<BizTx> {

    @Override
    public Class<BizTx> getEntityClass() {
        return BizTx.class;
    }
    private void setUp() {
        cacheable.put(RoleEnum.USER, true);
        cacheable.put(RoleEnum.ADMIN, true);
        cacheable.put(RoleEnum.APP, true);
        cacheable.put(RoleEnum.PUBLIC, true);
        cacheable.put(RoleEnum.ROOT, true);
    }
}

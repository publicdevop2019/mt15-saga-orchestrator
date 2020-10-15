package com.hw.aggregate.tx.model;

import com.hw.shared.sql.RestfulQueryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class BizTxQueryRegistry extends RestfulQueryRegistry<BizTx> {
    @Autowired
    private AppBizTxSelectQueryBuilder appBizTaskSelectQueryBuilder;
    @Autowired
    private AdminBizTxSelectQueryBuilder adminBizTaskSelectQueryBuilder;

    @Override
    @PostConstruct
    protected void configQueryBuilder() {
        selectQueryBuilder.put(RoleEnum.ADMIN, adminBizTaskSelectQueryBuilder);
        selectQueryBuilder.put(RoleEnum.APP, appBizTaskSelectQueryBuilder);
    }
}

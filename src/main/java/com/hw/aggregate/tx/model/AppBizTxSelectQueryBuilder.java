package com.hw.aggregate.tx.model;

import com.hw.shared.sql.builder.SelectQueryBuilder;
import org.springframework.stereotype.Component;

@Component
public class AppBizTxSelectQueryBuilder extends SelectQueryBuilder<BizTx> {
    {
        DEFAULT_PAGE_SIZE = 1;
        MAX_PAGE_SIZE = 1;
    }
}

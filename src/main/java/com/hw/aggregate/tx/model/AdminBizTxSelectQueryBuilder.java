package com.hw.aggregate.tx.model;

import com.hw.shared.sql.builder.SelectQueryBuilder;
import com.hw.shared.sql.clause.SelectFieldLongEqualClause;
import com.hw.shared.sql.clause.SelectFieldStringEqualClause;
import org.springframework.stereotype.Component;

import static com.hw.aggregate.tx.model.CreateOrderTask.*;

@Component
public class AdminBizTxSelectQueryBuilder extends SelectQueryBuilder<CreateOrderTask> {
    {
        supportedWhereField.put(ENTITY_TX_NAME, new SelectFieldStringEqualClause<>(ENTITY_TX_NAME));
        supportedWhereField.put(ENTITY_TX_STATUS, new SelectFieldStringEqualClause<>(ENTITY_TX_STATUS));
        supportedWhereField.put(ENTITY_REFERENCE_ID, new SelectFieldLongEqualClause<>(ENTITY_REFERENCE_ID));
        allowEmptyClause=true;
    }
}

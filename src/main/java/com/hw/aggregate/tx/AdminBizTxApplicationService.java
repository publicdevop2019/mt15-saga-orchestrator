package com.hw.aggregate.tx;

import com.hw.aggregate.tx.model.CreateOrderBizTx;
import com.hw.aggregate.tx.representation.AdminBizTxCardRep;
import com.hw.shared.rest.RoleBasedRestfulService;
import com.hw.shared.rest.VoidTypedClass;
import com.hw.shared.sql.RestfulQueryRegistry;
import org.springframework.stereotype.Service;

@Service
public class AdminBizTxApplicationService extends RoleBasedRestfulService<CreateOrderBizTx, AdminBizTxCardRep, Void, VoidTypedClass> {
    {
        entityClass = CreateOrderBizTx.class;
        role = RestfulQueryRegistry.RoleEnum.ADMIN;
    }

    @Override
    public AdminBizTxCardRep getEntitySumRepresentation(CreateOrderBizTx bizTask) {
        return new AdminBizTxCardRep(bizTask);
    }
}

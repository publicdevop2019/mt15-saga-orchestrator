package com.hw.aggregate.tx;

import com.hw.aggregate.tx.model.CreateOrderTask;
import com.hw.aggregate.tx.representation.AdminBizTxCardRep;
import com.hw.shared.rest.RoleBasedRestfulService;
import com.hw.shared.rest.VoidTypedClass;
import com.hw.shared.sql.RestfulQueryRegistry;
import org.springframework.stereotype.Service;

@Service
public class AdminTaskApplicationService extends RoleBasedRestfulService<CreateOrderTask, AdminBizTxCardRep, Void, VoidTypedClass> {
    {
        entityClass = CreateOrderTask.class;
        role = RestfulQueryRegistry.RoleEnum.ADMIN;
    }

    @Override
    public AdminBizTxCardRep getEntitySumRepresentation(CreateOrderTask bizTask) {
        return new AdminBizTxCardRep(bizTask);
    }
}

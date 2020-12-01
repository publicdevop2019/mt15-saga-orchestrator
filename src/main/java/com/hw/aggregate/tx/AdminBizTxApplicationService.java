package com.hw.aggregate.tx;

import com.hw.aggregate.tx.model.BizTx;
import com.hw.aggregate.tx.representation.AdminBizTxCardRep;
import com.hw.shared.rest.RoleBasedRestfulService;
import com.hw.shared.rest.VoidTypedClass;
import com.hw.shared.sql.RestfulQueryRegistry;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

@Service
public class AdminBizTxApplicationService extends RoleBasedRestfulService<BizTx, AdminBizTxCardRep, Void, VoidTypedClass> {
    {
        entityClass = BizTx.class;
        role = RestfulQueryRegistry.RoleEnum.ADMIN;
    }

    @Override
    public AdminBizTxCardRep getEntitySumRepresentation(BizTx bizTask) {
        return new AdminBizTxCardRep(bizTask);
    }
}

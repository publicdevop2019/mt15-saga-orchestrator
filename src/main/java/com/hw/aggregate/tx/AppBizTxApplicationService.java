package com.hw.aggregate.tx;

import com.hw.aggregate.tx.model.CreateOrderBizTx;
import com.hw.aggregate.tx.representation.AppBizTxRep;
import com.hw.shared.rest.CreatedAggregateRep;
import com.hw.shared.rest.RoleBasedRestfulService;
import com.hw.shared.rest.VoidTypedClass;
import com.hw.shared.sql.RestfulQueryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AppBizTxApplicationService extends RoleBasedRestfulService<CreateOrderBizTx, Void, AppBizTxRep, VoidTypedClass> {
    {
        entityClass = CreateOrderBizTx.class;
        role = RestfulQueryRegistry.RoleEnum.APP;
        rollbackSupported = false;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreatedAggregateRep create(Object command, String changeId) {
        log.debug("creating task");
        return super.create(command, changeId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replaceById(Long id, Object command, String changeId) {
        super.replaceById(id, command, changeId);
    }

    @Override
    public AppBizTxRep getEntityRepresentation(CreateOrderBizTx bizTask) {
        return new AppBizTxRep(bizTask);
    }

}

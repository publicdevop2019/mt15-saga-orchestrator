package com.hw.aggregate.tx;

import com.hw.aggregate.tx.command.AppCreateBizTxCommand;
import com.hw.aggregate.tx.command.AppUpdateBizTxCommand;
import com.hw.aggregate.tx.model.BizTx;
import com.hw.aggregate.tx.representation.AppBizTxRep;
import com.hw.shared.rest.CreatedAggregateRep;
import com.hw.shared.rest.DefaultRoleBasedRestfulService;
import com.hw.shared.rest.VoidTypedClass;
import com.hw.shared.sql.RestfulQueryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.Map;

@Slf4j
@Service
public class AppBizTxApplicationService extends DefaultRoleBasedRestfulService<BizTx, Void, AppBizTxRep, VoidTypedClass> {
    @Autowired
    private EntityManager entityManager;

    @PostConstruct
    private void setUp() {
        entityClass = BizTx.class;
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
    public BizTx replaceEntity(BizTx bizTask, Object command) {
        return bizTask.replace((AppUpdateBizTxCommand) command);
    }

    @Override
    public Void getEntitySumRepresentation(BizTx bizTask) {
        return null;
    }

    @Override
    public AppBizTxRep getEntityRepresentation(BizTx bizTask) {
        return new AppBizTxRep(bizTask);
    }

    @Override
    protected BizTx createEntity(long id, Object command) {
        return BizTx.create(id, (AppCreateBizTxCommand) command);
    }

    @Override
    public void preDelete(BizTx bizTask) {

    }

    @Override
    public void postDelete(BizTx bizTask) {

    }

    @Override
    protected void prePatch(BizTx bizTask, Map<String, Object> params, VoidTypedClass middleLayer) {

    }

    @Override
    protected void postPatch(BizTx bizTask, Map<String, Object> params, VoidTypedClass middleLayer) {

    }
}

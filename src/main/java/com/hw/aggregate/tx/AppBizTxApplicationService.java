package com.hw.aggregate.tx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.aggregate.tx.command.AppCreateBizTxCommand;
import com.hw.aggregate.tx.command.AppUpdateBizTxCommand;
import com.hw.aggregate.tx.model.BizTx;
import com.hw.aggregate.tx.model.BizTxQueryRegistry;
import com.hw.aggregate.tx.representation.AppBizTxRep;
import com.hw.shared.IdGenerator;
import com.hw.shared.idempotent.AppChangeRecordApplicationService;
import com.hw.shared.rest.CreatedEntityRep;
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
    private BizTxQueryRegistry registry;
    @Autowired
    private IdGenerator idGenerator2;
    @Autowired
    private AppChangeRecordApplicationService changeRepository2;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BizTxRepository repo2;
    @Autowired
    private EntityManager entityManager;

    @PostConstruct
    private void setUp() {
        repo = repo2;
        queryRegistry = registry;
        entityClass = BizTx.class;
        role = RestfulQueryRegistry.RoleEnum.APP;
        idGenerator = idGenerator2;
        appChangeRecordApplicationService = changeRepository2;
        om = objectMapper;
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreatedEntityRep create(Object command, String changeId) {
        log.debug("creating task");
        return super.create(command,changeId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replaceById(Long id, Object command, String changeId) {
        super.replaceById(id,command,changeId);
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

    public void rollbackTransaction(String transactionId) {

    }
}

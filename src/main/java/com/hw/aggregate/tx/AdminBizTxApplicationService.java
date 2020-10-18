package com.hw.aggregate.tx;

import com.hw.aggregate.tx.model.BizTx;
import com.hw.aggregate.tx.representation.AdminBizTxCardRep;
import com.hw.shared.rest.DefaultRoleBasedRestfulService;
import com.hw.shared.rest.VoidTypedClass;
import com.hw.shared.sql.RestfulQueryRegistry;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

@Service
public class AdminBizTxApplicationService extends DefaultRoleBasedRestfulService<BizTx, AdminBizTxCardRep, Void, VoidTypedClass> {

    @PostConstruct
    private void setUp() {
        entityClass = BizTx.class;
        role = RestfulQueryRegistry.RoleEnum.ADMIN;
    }

    @Override
    public BizTx replaceEntity(BizTx bizTask, Object command) {
        return null;
    }

    @Override
    public AdminBizTxCardRep getEntitySumRepresentation(BizTx bizTask) {
        return new AdminBizTxCardRep(bizTask);
    }

    @Override
    public Void getEntityRepresentation(BizTx bizTask) {
        return null;
    }

    @Override
    protected BizTx createEntity(long id, Object command) {
        return null;
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

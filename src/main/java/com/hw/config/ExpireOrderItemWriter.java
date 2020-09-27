package com.hw.config;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExpireOrderItemWriter implements ItemWriter<CreateBizStateMachineCommand> {

    @Override
    public void write(List<? extends CreateBizStateMachineCommand> list) throws Exception {

    }
}

package com.hw.config;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@JobScope
public class ExpireOrderItemReader implements ItemReader<CreateBizStateMachineCommand> {

    @Autowired
    ReleaseJobContext releaseJobContext;

    String listKey;

    @Value("#{jobParameters['list']}")
    public void setFileName(final String name) {
        listKey = name;
    }

    @Override
    public CreateBizStateMachineCommand read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        List<CreateBizStateMachineCommand> createBizStateMachineCommands = releaseJobContext.getJobList().get(listKey);
        AtomicInteger atomicInteger = releaseJobContext.getJobIndex().get(listKey);
        int i = atomicInteger.incrementAndGet();
        if (i >= createBizStateMachineCommands.size()) {
            return null;
        }
        return createBizStateMachineCommands.get(i);
    }
}

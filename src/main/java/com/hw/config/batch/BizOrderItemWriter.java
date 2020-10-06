package com.hw.config.batch;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BizOrderItemWriter implements ItemWriter<CreateBizStateMachineCommand> {

    @Override
    public void write(List<? extends CreateBizStateMachineCommand> list) throws Exception {

    }
}

package com.hw.config.batch;

import com.hw.aggregate.sm.AppBizStateMachineApplicationService;
import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
public class BizOrderProcessor implements ItemProcessor<CreateBizStateMachineCommand, CreateBizStateMachineCommand> {
    @Autowired
    AppBizStateMachineApplicationService appBizStateMachineApplicationService;

    @Override
    public CreateBizStateMachineCommand process(CreateBizStateMachineCommand bizOrder) throws Exception {
        log.info("start processing order {}", bizOrder.getOrderId());
        appBizStateMachineApplicationService.start(bizOrder);
        return null;
    }
}

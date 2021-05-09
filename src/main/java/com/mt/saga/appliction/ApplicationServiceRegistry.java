package com.mt.saga.appliction;

import com.mt.saga.appliction.order_state_machine.OrderStateMachineApplicationService;
import com.mt.saga.appliction.task.TaskApplicationService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApplicationServiceRegistry {
    @Getter
    private static OrderStateMachineApplicationService stateMachineApplicationService;
    @Getter
    private static TaskApplicationService taskApplicationService;
    @Autowired
    private void setStateMachineApplicationService(OrderStateMachineApplicationService stateMachineApplicationService){
        ApplicationServiceRegistry.stateMachineApplicationService=stateMachineApplicationService;
    }
    @Autowired
    private void setTaskApplicationService(TaskApplicationService taskApplicationService){
        ApplicationServiceRegistry.taskApplicationService=taskApplicationService;
    }
}

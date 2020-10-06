package com.hw.aggregate.sm;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(produces = "application/json", path = "sm")
public class BizStateMachineController {
    @Autowired
    private AppBizStateMachineApplicationService appBizStateMachineApplicationService;

    @PostMapping("app")
    public ResponseEntity<Void> createForApp(@RequestBody List<CreateBizStateMachineCommand> commands) {
//        if (commands.size() > 1) {
            appBizStateMachineApplicationService.startBatch(commands);
//        } else {
//            appBizStateMachineApplicationService.start(commands.get(0));
//        }
        return ResponseEntity.ok().build();
    }
}

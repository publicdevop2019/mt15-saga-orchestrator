package com.hw.config.batch;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import lombok.Data;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ReleaseJobContext {
    private ConcurrentHashMap<String, List<CreateBizStateMachineCommand>> jobList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, AtomicInteger> jobIndex = new ConcurrentHashMap<>();
}

package com.mt.saga.domain.model.task;

import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public interface MessengerService {
    @Async
    void notifyBusinessOwner(Map<String, String> contentMap);
}

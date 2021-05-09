package com.mt.saga.port.adapter.http;

import com.mt.common.domain.model.service_discovery.EurekaHelper;
import com.mt.saga.domain.model.task.MessengerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class HttpMessengerService implements MessengerService {

    @Value("${mt.url.messenger.notify}")
    private String notifyUrl;

    @Value("${mt.discovery.messenger}")
    private String appName;
    @Autowired
    private EurekaHelper eurekaHelper;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Async
    public void notifyBusinessOwner(Map<String, String> contentMap) {
        log.info("starting notifyBusinessOwner");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> mapHttpEntity = new HttpEntity<>(contentMap, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + notifyUrl, HttpMethod.POST, mapHttpEntity, String.class);
        log.info("complete notifyBusinessOwner");
    }
}

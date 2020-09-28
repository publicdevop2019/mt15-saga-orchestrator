package com.hw.aggregate.sm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.shared.EurekaRegistryHelper;
import com.hw.shared.ResourceServiceTokenHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class MessengerService {

    @Autowired
    private EurekaRegistryHelper eurekaRegistryHelper;

    @Value("${url.notify}")
    private String notifyUrl;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ResourceServiceTokenHelper tokenHelper;

    @Async
    public void notifyBusinessOwner(Map<String, String> contentMap) {
        log.info("starting notifyBusinessOwner");
        String body = null;
        try {
            body = mapper.writeValueAsString(contentMap);
        } catch (JsonProcessingException e) {
            /**
             * this block is purposely left blank
             */
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(body, headers);
        tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + notifyUrl, HttpMethod.POST, hashMapHttpEntity, String.class);
        log.info("complete notifyBusinessOwner");
    }
}

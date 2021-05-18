package com.mt.saga.port.adapter.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.common.domain.model.restful.SumPagedRep;
import com.mt.common.domain.model.service_discovery.EurekaHelper;
import com.mt.saga.domain.model.task.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

import static com.mt.common.CommonConstant.HTTP_HEADER_CHANGE_ID;
import static com.mt.common.CommonConstant.HTTP_PARAM_QUERY;

@Service
@Slf4j
public class HttpPaymentService implements PaymentService {

    @Value("${mt.url.payment.wechat.confirm}")
    private String confirmUrl;

    @Value("${mt.url.payment.wechat.create}")
    private String paymentUrl;

    @Value("${mt.url.payment.change}")
    private String changeUrl;
    @Value("${mt.app.name.mt6}")
    private String appName;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private EurekaHelper eurekaHelper;

    @Override
    public void cancelPaymentLink(String cancelTxId, String txId) {
        if (hasChange(txId)) {
            //to be impl in payment
//            log.info("starting rollbackTransaction");
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
//            String applicationUrl = eurekaHelper.getApplicationUrl(appName);
//            log.debug("target url {}", applicationUrl);
//            restTemplate.exchange(applicationUrl + changeUrl + "?" + HTTP_PARAM_QUERY + "=" + HTTP_HEADER_CHANGE_ID + ":" + cancelTxId, HttpMethod.DELETE, hashMapHttpEntity, String.class);
//            log.info("complete rollbackTransaction");
        }
    }

    @Override
    public String generatePaymentLink(String orderId, String changeId) {
        log.info("starting generatePaymentLink");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, changeId);
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("orderId", orderId);
        String body = null;
        try {
            body = mapper.writeValueAsString(stringStringHashMap);
        } catch (JsonProcessingException e) {
            /**
             * this block is purposely left blank
             */
        }
        ParameterizedTypeReference<HashMap<String, String>> responseType =
                new ParameterizedTypeReference<>() {
                };
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(body, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        log.debug("target url {}", applicationUrl);
        ResponseEntity<HashMap<String, String>> exchange = restTemplate.exchange(applicationUrl + paymentUrl, HttpMethod.POST, hashMapHttpEntity, responseType);
        String result = null;
        if (exchange.getBody() != null) {
            result = exchange.getBody().get("paymentLink");
        } else {
            log.error("unable to extract payment link from response");
        }
        log.info("complete generatePaymentLink");
        return result;
    }

    @Override
    public Boolean confirmPaymentStatus(String orderId) {
        log.info("starting confirmPaymentStatus");
        ParameterizedTypeReference<HashMap<String, Boolean>> responseType =
                new ParameterizedTypeReference<>() {
                };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        ResponseEntity<HashMap<String, Boolean>> exchange = restTemplate.exchange(applicationUrl + confirmUrl + "/" + orderId, HttpMethod.GET, hashMapHttpEntity, responseType);
        Boolean result = null;
        if (exchange.getBody() != null) {
            result = exchange.getBody().get("paymentStatus");
        } else {
            log.error("unable to extract paymentStatus from response");
        }
        log.info("complete confirmPaymentStatus");
        return result;

    }

    private boolean hasChange(String changeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        ResponseEntity<SumPagedRep> exchange = restTemplate.exchange(applicationUrl + changeUrl + "?" + HTTP_PARAM_QUERY + "=" + "changeId:" + changeId, HttpMethod.GET, hashMapHttpEntity, SumPagedRep.class);
        return exchange.getBody().getData().size() == 1;
    }
}

package com.mt.saga.port.adapter.http;

import com.mt.common.domain.model.restful.SumPagedRep;
import com.mt.common.domain.model.service_discovery.EurekaHelper;
import com.mt.saga.domain.model.task.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

import static com.mt.common.CommonConstant.HTTP_HEADER_CHANGE_ID;
import static com.mt.common.CommonConstant.HTTP_PARAM_QUERY;
import static com.mt.common.domain.model.audit.Auditable.ENTITY_CREATED_BY;


@Slf4j
@Service
public class HttpCartService implements CartService {

    @Value("${mt.url.profile.cart.clean}")
    private String url;

    @Value("${mt.url.profile.change}")
    private String changeUrl;
    @Value("${mt.discovery.profile}")
    private String appName;
    @Autowired
    private EurekaHelper eurekaHelper;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void cancelClearCart(String userId, Set<String> cartIds, String cancelTxId, String txId) {
        log.info("start of cancel clear cart");
        if (hasChange(txId)) {
            updateCart(userId, cartIds, cancelTxId, HttpMethod.POST);
        }
        log.info("end of cancel clear cart");
    }

    @Override
    public void clearCart(String userId, Set<String> cartIds, String changeId) {
        log.info("start of clear cart");
        updateCart(userId, cartIds, changeId, HttpMethod.DELETE);
        log.info("end of clear cart");
    }

    private void updateCart(String userId, Set<String> cartIds, String changeId, HttpMethod httpMethod) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        String query = "id:" + String.join(".", cartIds);
        restTemplate.exchange(applicationUrl + url + "?" + HTTP_PARAM_QUERY + "=" + ENTITY_CREATED_BY + ":" + userId + "," + query, httpMethod, hashMapHttpEntity, Void.class);
    }

    private boolean hasChange(String changeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        ResponseEntity<SumPagedRep> exchange = restTemplate.exchange(applicationUrl + changeUrl + "?" + HTTP_PARAM_QUERY + "=" + "entityType:BizCart,changeId:" + changeId, HttpMethod.GET, hashMapHttpEntity, SumPagedRep.class);
        return exchange.getBody().getData().size() == 1;
    }
}

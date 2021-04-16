package com.hw.aggregate.sm;

import com.hw.config.EurekaHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

import static com.hw.shared.AppConstant.HTTP_HEADER_CHANGE_ID;
import static com.hw.shared.AppConstant.HTTP_PARAM_QUERY;
import static com.hw.shared.Auditable.ENTITY_CREATED_BY;

@Slf4j
@Service
public class CartService {

    @Value("${mt.url.profile.cart.clean}")
    private String url;

    @Value("${mt.url.profile.change.rollback}")
    private String changeUrl;
    @Value("${mt.discovery.profile}")
    private String appName;
    @Autowired
    private EurekaHelper eurekaHelper;
    @Autowired
    private RestTemplate restTemplate;

    public void rollbackTransaction(String changeId) {
        log.info("starting rollbackTransaction");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + changeUrl + "?" + HTTP_PARAM_QUERY + "=" + HTTP_HEADER_CHANGE_ID + ":" + changeId, HttpMethod.DELETE, hashMapHttpEntity, String.class);
        log.info("complete rollbackTransaction");
    }

    public void clearCart(String userId, Set<String> cartIds, String changeId) {
        log.info("starting clearCart");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        String query = "id:" + String.join(".", cartIds);
        restTemplate.exchange(applicationUrl + url + "?" + HTTP_PARAM_QUERY + "=" + ENTITY_CREATED_BY + ":" + userId + "," + query, HttpMethod.DELETE, hashMapHttpEntity, Void.class);
        log.info("complete clearCart");
    }
}

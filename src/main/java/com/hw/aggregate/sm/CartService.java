package com.hw.aggregate.sm;

import com.hw.shared.EurekaRegistryHelper;
import com.hw.shared.ResourceServiceTokenHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import static com.hw.shared.AppConstant.HTTP_HEADER_CHANGE_ID;
import static com.hw.shared.AppConstant.HTTP_PARAM_QUERY;
import static com.hw.shared.Auditable.ENTITY_CREATED_BY;

@Slf4j
@Service
public class CartService {
    @Autowired
    private EurekaRegistryHelper eurekaRegistryHelper;

    @Value("${url.cart.app}")
    private String url;

    @Value("${url.cart.change.app}")
    private String changeUrl;

    @Autowired
    private ResourceServiceTokenHelper tokenHelper;

    public void rollbackTransaction(String changeId) {
        log.info("starting rollbackTransaction");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + changeUrl + "?" + HTTP_PARAM_QUERY + "=" + HTTP_HEADER_CHANGE_ID + ":" + changeId, HttpMethod.DELETE, hashMapHttpEntity, String.class);
        log.info("complete rollbackTransaction");
    }

    public void clearCart(String userId, String changeId) {
        log.info("starting clearCart");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + url + "?" + HTTP_PARAM_QUERY + "=" + ENTITY_CREATED_BY + ":" + userId, HttpMethod.DELETE, hashMapHttpEntity, Void.class);
        log.info("complete clearCart");
    }
}

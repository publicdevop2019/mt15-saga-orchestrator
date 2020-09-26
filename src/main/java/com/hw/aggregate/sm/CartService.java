package com.hw.aggregate.sm;

import com.hw.shared.EurekaRegistryHelper;
import com.hw.shared.ResourceServiceTokenHelper;
import com.hw.shared.sql.PatchCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;

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
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + changeUrl + "?" + HTTP_PARAM_QUERY + "=" + HTTP_HEADER_CHANGE_ID + ":" + changeId, HttpMethod.DELETE, hashMapHttpEntity, String.class);
        } catch (Exception e) {
            log.error("rollbackTransaction", e);
            throw e;
        }
    }

    public void clearCart(long userId, String changeId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HTTP_HEADER_CHANGE_ID,changeId);
            HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + url + "?" + HTTP_PARAM_QUERY + "=" + ENTITY_CREATED_BY + ":" + userId, HttpMethod.DELETE, hashMapHttpEntity, Void.class);
        } catch (Exception e) {
            log.error("clear cart", e);
            throw e;
        }
    }
}

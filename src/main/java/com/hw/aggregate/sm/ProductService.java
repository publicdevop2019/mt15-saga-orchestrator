package com.hw.aggregate.sm;

import com.hw.config.EurekaHelper;
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
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.hw.shared.AppConstant.HTTP_HEADER_CHANGE_ID;
import static com.hw.shared.AppConstant.HTTP_PARAM_QUERY;

@Service
@Slf4j
public class ProductService {

    @Value("${mt.url.mall.product}")
    private String productUrl;

    @Value("${mt.url.mall.change}")
    private String changeUrl;

    @Autowired
    private EurekaHelper eurekaHelper;
    @Autowired
    private RestTemplate restTemplate;

    @Value("${mt.discovery.mall}")
    private String appName;

    public void updateProductStorage(List<PatchCommand> changeList, String txId) {
        log.info("starting updateProductStorage");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, txId);
        HttpEntity<List<PatchCommand>> hashMapHttpEntity = new HttpEntity<>(changeList, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + productUrl, HttpMethod.PATCH, hashMapHttpEntity, String.class);
        log.info("complete updateProductStorage");
    }

    public void rollbackTransaction(String changeId) {
        log.info("starting rollbackTransaction");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + changeUrl + "?" + HTTP_PARAM_QUERY + "=" + HTTP_HEADER_CHANGE_ID + ":" + changeId, HttpMethod.DELETE, hashMapHttpEntity, String.class);
        log.info("complete rollbackTransaction");
    }

}

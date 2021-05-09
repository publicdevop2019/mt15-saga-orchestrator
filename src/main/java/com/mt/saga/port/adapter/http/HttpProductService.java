package com.mt.saga.port.adapter.http;

import com.mt.common.domain.model.restful.PatchCommand;
import com.mt.common.domain.model.restful.SumPagedRep;
import com.mt.common.domain.model.service_discovery.EurekaHelper;
import com.mt.saga.domain.model.order_state_machine.order.CartDetail;
import com.mt.saga.domain.model.order_state_machine.product.ProductsSummary;
import com.mt.saga.domain.model.task.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static com.mt.common.CommonConstant.HTTP_HEADER_CHANGE_ID;
import static com.mt.common.CommonConstant.HTTP_PARAM_QUERY;


@Service
@Slf4j
public class HttpProductService extends ProductService {

    @Value("${mt.url.mall.skus}")
    private String skusUrl;
    @Value("${mt.url.mall.products}")
    private String productUrl;

    @Value("${mt.url.mall.change}")
    private String changeUrl;

    @Autowired
    private EurekaHelper eurekaHelper;
    @Autowired
    private RestTemplate restTemplate;

    @Value("${mt.discovery.mall}")
    private String appName;

    @Override
    public void updateProductStorage(List<PatchCommand> changeList, String txId) {
        log.info("starting updateProductStorage");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, txId);
        HttpEntity<List<PatchCommand>> hashMapHttpEntity = new HttpEntity<>(changeList, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + skusUrl, HttpMethod.PATCH, hashMapHttpEntity, String.class);
        log.info("complete updateProductStorage");
    }

    @Override
    public void cancelUpdateProductStorage(List<PatchCommand> originalChange, String cancelTxId, String txId) {
        if (hasChange(txId)) {
            updateProductStorage(PatchCommand.buildRollbackCommand(originalChange), cancelTxId);
        }
    }

    @Override
    public boolean validateOrderedProduct(List<CartDetail> customerOrderItemList) {
        log.info("start of validate order");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<CartDetail>> hashMapHttpEntity = new HttpEntity<>(customerOrderItemList, headers);

        List<String> collect = customerOrderItemList.stream().map(CartDetail::getProductId).collect(Collectors.toList());

        String query = "?" + HTTP_PARAM_QUERY + "=id:" + String.join(".", collect);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);

        ResponseEntity<ProductsSummary> exchange = restTemplate.exchange(applicationUrl + productUrl + query, HttpMethod.GET, hashMapHttpEntity, ProductsSummary.class);
        ProductsSummary body = exchange.getBody();
        boolean b = validateProducts(body, customerOrderItemList);
        log.info("end of validate order, result is {}", b);
        return b;
    }


    private boolean hasChange(String changeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        ResponseEntity<SumPagedRep> exchange = restTemplate.exchange(applicationUrl + changeUrl + "?" + HTTP_PARAM_QUERY + "=" + "entityType:Sku,changeId:" + changeId, HttpMethod.GET, hashMapHttpEntity, SumPagedRep.class);
        return exchange.getBody().getData().size() == 1;
    }

}

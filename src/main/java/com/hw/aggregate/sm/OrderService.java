package com.hw.aggregate.sm;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.model.order.*;
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

import java.util.List;

import static com.hw.shared.AppConstant.HTTP_HEADER_CHANGE_ID;
import static com.hw.shared.AppConstant.HTTP_PARAM_QUERY;

@Slf4j
@Service
public class OrderService {

    @Value("${mt.url.profile.order.validate}")
    private String orderValidateUrl;
    @Value("${mt.url.profile.order.create}")
    private String orderUrl;

    @Value("${mt.url.profile.change.rollback}")
    private String changeUrl;
    @Value("${mt.discovery.profile}")
    private String appName;
    @Autowired
    private EurekaHelper eurekaHelper;
    @Autowired
    private RestTemplate restTemplate;

    public void validateOrder(List<BizOrderItem> productList) {
        log.info("starting validateOrder");
        HttpHeaders headers = new HttpHeaders();
        AppValidateBizOrderCommand appValidateBizOrderCommand = new AppValidateBizOrderCommand();
        appValidateBizOrderCommand.setProductList(productList);
        HttpEntity<AppValidateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appValidateBizOrderCommand, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderValidateUrl, HttpMethod.POST, hashMapHttpEntity, String.class);
        log.info("complete validateOrder");
    }

    public void saveConcludeOrder(CreateBizStateMachineCommand machineCommand, BizOrderStatus status) {
        log.info("starting saveConcludeOrder");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, machineCommand.getTxId());
        AppUpdateBizOrderCommand appCreateBizOrderCommand = new AppUpdateBizOrderCommand();
        appCreateBizOrderCommand.setOrderId(machineCommand.getOrderId());
        appCreateBizOrderCommand.setOrderState(status);
        appCreateBizOrderCommand.setPaymentStatus(Boolean.TRUE);
        appCreateBizOrderCommand.setVersion(machineCommand.getVersion());
        HttpEntity<AppUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appCreateBizOrderCommand, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl + "/" + machineCommand.getOrderId(), HttpMethod.PUT, hashMapHttpEntity, String.class);
        log.info("complete saveConcludeOrder");
    }

    public void saveReservedOrder(CreateBizStateMachineCommand machineCommand, BizOrderStatus status) {
        log.info("starting saveReservedOrder");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, machineCommand.getTxId());
        AppUpdateBizOrderCommand appCreateBizOrderCommand = new AppUpdateBizOrderCommand();
        appCreateBizOrderCommand.setOrderId(machineCommand.getOrderId());
        appCreateBizOrderCommand.setOrderState(status);
        appCreateBizOrderCommand.setVersion(machineCommand.getVersion());
        HttpEntity<AppUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appCreateBizOrderCommand, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl + "/" + machineCommand.getOrderId(), HttpMethod.PUT, hashMapHttpEntity, String.class);
        log.info("complete saveReservedOrder");
    }

    public void saveNewOrder(String paymentLink, BizOrderStatus status, CreateBizStateMachineCommand command) {
        log.info("starting saveNewOrder");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, command.getTxId());
        AppCreateBizOrderCommand appCreateBizOrderCommand = new AppCreateBizOrderCommand();
        appCreateBizOrderCommand.setAddress(command.getAddress());
        appCreateBizOrderCommand.setCreatedBy(command.getCreatedBy());
        appCreateBizOrderCommand.setOrderId(command.getOrderId());
        appCreateBizOrderCommand.setOrderState(status);
        appCreateBizOrderCommand.setPaymentAmt(command.getPaymentAmt());
        appCreateBizOrderCommand.setPaymentType(command.getPaymentType());
        appCreateBizOrderCommand.setPaymentLink(paymentLink);
        appCreateBizOrderCommand.setProductList(command.getProductList());
        appCreateBizOrderCommand.setUserId(command.getUserId());
        HttpEntity<AppCreateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appCreateBizOrderCommand, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl, HttpMethod.POST, hashMapHttpEntity, String.class);
        log.info("complete saveNewOrder");
    }

    public void savePaidOrder(CreateBizStateMachineCommand command, BizOrderStatus status) {
        log.info("starting savePaidOrder");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, command.getTxId());
        AppUpdateBizOrderCommand appCreateBizOrderCommand = new AppUpdateBizOrderCommand();
        appCreateBizOrderCommand.setOrderId(command.getOrderId());
        appCreateBizOrderCommand.setOrderState(status);
        appCreateBizOrderCommand.setPaymentStatus(Boolean.TRUE);
        appCreateBizOrderCommand.setVersion(command.getVersion());
        HttpEntity<AppUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appCreateBizOrderCommand, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl + "/" + command.getOrderId(), HttpMethod.PUT, hashMapHttpEntity, String.class);
        log.info("complete savePaidOrder");
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

    public void saveRecycleOrder(CreateBizStateMachineCommand command, BizOrderStatus orderStatus) {
        log.info("starting saveRecycleOrder");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, command.getTxId());
        AppUpdateBizOrderCommand appCreateBizOrderCommand = new AppUpdateBizOrderCommand();
        appCreateBizOrderCommand.setOrderId(command.getOrderId());
        appCreateBizOrderCommand.setOrderState(orderStatus);
        appCreateBizOrderCommand.setVersion(command.getVersion());
        HttpEntity<AppUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appCreateBizOrderCommand, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl + "/" + command.getOrderId(), HttpMethod.PUT, hashMapHttpEntity, String.class);
        log.info("complete saveRecycleOrder");
    }
}

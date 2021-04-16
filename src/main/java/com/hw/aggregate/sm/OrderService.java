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

    public void validateOrder(List<CartDetail> productList, String orderId) {
        log.info("starting validateOrder");
        HttpHeaders headers = new HttpHeaders();
        AppValidateBizOrderCommand appValidateBizOrderCommand = new AppValidateBizOrderCommand();
        appValidateBizOrderCommand.setProductList(productList);
        appValidateBizOrderCommand.setOrderId(orderId);
        HttpEntity<AppValidateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appValidateBizOrderCommand, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderValidateUrl, HttpMethod.POST, hashMapHttpEntity, String.class);
        log.info("complete validateOrder");
    }

    public void saveConcludeOrder(CreateBizStateMachineCommand machineCommand) {
        updateOrder(machineCommand, AppUpdateBizOrderCommand.CommandType.CONCLUDE);
    }

    private void updateOrder(CreateBizStateMachineCommand machineCommand, AppUpdateBizOrderCommand.CommandType commandType) {
        log.info("starting update order to {}", commandType);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, machineCommand.getTxId());
        AppUpdateBizOrderCommand command = new AppUpdateBizOrderCommand();
        command.setOrderId(machineCommand.getOrderId());
        command.setCommandType(commandType);
        command.setVersion(machineCommand.getVersion());
        HttpEntity<AppUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(command, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl + "/" + machineCommand.getOrderId(), HttpMethod.PUT, hashMapHttpEntity, String.class);
        log.info("starting update order to {}", commandType);
    }

    public void saveReservedOrder(CreateBizStateMachineCommand machineCommand) {
        updateOrder(machineCommand, AppUpdateBizOrderCommand.CommandType.CANCEL_RECYCLE);
    }

    public void saveNewOrder(String paymentLink, CreateBizStateMachineCommand command) {
        log.info("starting saveNewOrder");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, command.getTxId());
        AppCreateBizOrderCommand appCreateBizOrderCommand = new AppCreateBizOrderCommand();
        appCreateBizOrderCommand.setAddress(command.getAddress());
        appCreateBizOrderCommand.setCreatedBy(command.getCreatedBy());
        appCreateBizOrderCommand.setOrderId(command.getOrderId());
        appCreateBizOrderCommand.setOrderState(BizOrderStatus.DRAFT);
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

    public void savePaidOrder(CreateBizStateMachineCommand command) {
        updateOrder(command, AppUpdateBizOrderCommand.CommandType.CONFIRM_PAYMENT);
    }

    public void saveRecycleOrder(CreateBizStateMachineCommand command) {
        updateOrder(command, AppUpdateBizOrderCommand.CommandType.RECYCLE);
    }
}

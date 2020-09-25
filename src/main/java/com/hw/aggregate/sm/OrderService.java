package com.hw.aggregate.sm;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.model.order.*;
import com.hw.shared.EurekaRegistryHelper;
import com.hw.shared.ResourceServiceTokenHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
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

@Slf4j
@Service
public class OrderService {
    @Autowired
    private EurekaRegistryHelper eurekaRegistryHelper;

    @Value("${url.orders.app.validate}")
    private String orderValidateUrl;
    @Value("${url.orders.app}")
    private String orderUrl;

    @Value("${url.orders.change.app}")
    private String changeUrl;

    @Autowired
    private ResourceServiceTokenHelper tokenHelper;

    public void validateOrder(List<BizOrderItem> productList) {
        try {
            HttpHeaders headers = new HttpHeaders();
            AppValidateBizOrderCommand appValidateBizOrderCommand = new AppValidateBizOrderCommand();
            appValidateBizOrderCommand.setProductList(productList);
            HttpEntity<AppValidateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appValidateBizOrderCommand, headers);
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + orderValidateUrl, HttpMethod.POST, hashMapHttpEntity, String.class);
        } catch (Exception e) {
            log.error("validateOrder", e);
            throw e;
        }
    }

    public void saveConcludeOrder(CreateBizStateMachineCommand machineCommand, BizOrderStatus status) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HTTP_HEADER_CHANGE_ID, machineCommand.getTxId());
            AppUpdateBizOrderCommand appCreateBizOrderCommand = new AppUpdateBizOrderCommand();
            appCreateBizOrderCommand.setOrderId(machineCommand.getOrderId());
            appCreateBizOrderCommand.setOrderState(status);
            appCreateBizOrderCommand.setPaymentStatus(Boolean.TRUE);
            HttpEntity<AppUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appCreateBizOrderCommand, headers);
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + orderUrl, HttpMethod.PUT, hashMapHttpEntity, String.class);
        } catch (Exception e) {
            log.error("updateOrder", e);
            throw e;
        }
    }

    public void saveReservedOrder(CreateBizStateMachineCommand machineCommand, BizOrderStatus status) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HTTP_HEADER_CHANGE_ID, machineCommand.getTxId());
            AppUpdateBizOrderCommand appCreateBizOrderCommand = new AppUpdateBizOrderCommand();
            appCreateBizOrderCommand.setOrderId(machineCommand.getOrderId());
            appCreateBizOrderCommand.setOrderState(status);
            HttpEntity<AppUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appCreateBizOrderCommand, headers);
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + orderUrl, HttpMethod.PUT, hashMapHttpEntity, String.class);
        } catch (Exception e) {
            log.error("updateOrder", e);
            throw e;
        }
    }

    public void saveNewOrder(String paymentLink, BizOrderStatus status, CreateBizStateMachineCommand command) {
        try {
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
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + orderUrl, HttpMethod.POST, hashMapHttpEntity, String.class);
        } catch (Exception e) {
            log.error("updateOrder", e);
            throw e;
        }
    }

    public void savePaidOrder(CreateBizStateMachineCommand command, BizOrderStatus status) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HTTP_HEADER_CHANGE_ID, command.getTxId());
            AppUpdateBizOrderCommand appCreateBizOrderCommand = new AppUpdateBizOrderCommand();
            appCreateBizOrderCommand.setOrderId(command.getOrderId());
            appCreateBizOrderCommand.setOrderState(status);
            appCreateBizOrderCommand.setPaymentStatus(Boolean.TRUE);
            HttpEntity<com.hw.aggregate.sm.model.order.AppUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appCreateBizOrderCommand, headers);
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + orderUrl, HttpMethod.POST, hashMapHttpEntity, String.class);
        } catch (Exception e) {
            log.error("updateOrder", e);
            throw e;
        }
    }

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
}

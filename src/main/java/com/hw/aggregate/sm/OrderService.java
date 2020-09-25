package com.hw.aggregate.sm;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.model.order.AppCreateBizOrderCommand;
import com.hw.aggregate.sm.model.order.AppValidateBizOrderCommand;
import com.hw.aggregate.sm.model.order.BizOrderItem;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
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

    @Value("${url.orders.app}")
    private String orderUrl;
    @Value("${url.orders.app.create}")
    private String orderUrl2;

    @Value("${url.orders.change.app}")
    private String changeUrl;

    @Autowired
    private ResourceServiceTokenHelper tokenHelper;

    public void validateOrder(List<BizOrderItem> productList) {
        try {
            HttpHeaders headers = new HttpHeaders();
            AppValidateBizOrderCommand appValidateBizOrderCommand = new AppValidateBizOrderCommand();
            appValidateBizOrderCommand.setProductList(productList);
            HttpEntity<AppValidateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appValidateBizOrderCommand,headers);
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + orderUrl , HttpMethod.POST, hashMapHttpEntity, String.class);
        } catch (Exception e) {
            log.error("validateOrder", e);
            throw e;
        }
    }

    public void saveOrder(long id, String paymentLink, BizOrderStatus status, boolean paymentStatus, String changeId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HTTP_HEADER_CHANGE_ID, changeId);
            AppCreateBizOrderCommand appCreateBizOrderCommand = new AppCreateBizOrderCommand();
//            appCreateBizOrderCommand.setAddress(command.getAddress());
//            appCreateBizOrderCommand.setCreatedBy(command.getCreatedBy());
//            appCreateBizOrderCommand.setOrderId(command.getOrderId());
//            appCreateBizOrderCommand.setOrderState(status);
//            appCreateBizOrderCommand.setPaymentAmt(command.getPaymentAmt());
//            appCreateBizOrderCommand.setPaymentType(command.getPaymentType());
//            appCreateBizOrderCommand.setPaymentLink(paymentLink);
//            appCreateBizOrderCommand.setProductList(command.getProductList());
//            appCreateBizOrderCommand.setUserId(command.getUserId());
            HttpEntity<AppCreateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(appCreateBizOrderCommand, headers);
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + orderUrl , HttpMethod.PATCH, hashMapHttpEntity, String.class);
        } catch (Exception e) {
            log.error("updateOrder", e);
            throw e;
        }
    }
    public void saveOrder(String paymentLink, BizOrderStatus status, CreateBizStateMachineCommand command) {
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
            tokenHelper.exchange(eurekaRegistryHelper.getProxyHomePageUrl() + orderUrl2 , HttpMethod.POST, hashMapHttpEntity, String.class);
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

    @Data
    @AllArgsConstructor
    public static class AppUpdateBizOrderCommand {
        private String paymentLink;
        private BizOrderStatus status;
        private boolean paymentStatus;

    }
}

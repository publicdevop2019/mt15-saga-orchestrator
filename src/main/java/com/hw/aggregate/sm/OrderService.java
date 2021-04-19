package com.hw.aggregate.sm;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.model.order.AppCreateBizOrderCommand;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import com.hw.aggregate.sm.model.order.CancelUpdateBizOrderCommand;
import com.hw.aggregate.sm.model.order.UpdateBizOrderCommand;
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

import static com.hw.shared.AppConstant.HTTP_HEADER_CHANGE_ID;

@Slf4j
@Service
public class OrderService {

    @Value("${mt.url.profile.order.validate}")
    private String orderValidateUrl;
    @Value("${mt.url.profile.order.create}")
    private String orderUrl;

    @Value("${mt.url.profile.change}")
    private String changeUrl;
    @Value("${mt.discovery.profile}")
    private String appName;
    @Autowired
    private EurekaHelper eurekaHelper;
    @Autowired
    private RestTemplate restTemplate;

    public void concludeOrder(CreateBizStateMachineCommand command) {
        UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
        updateBizOrderCommand.setActualStorage(true);
        updateBizOrderCommand.setConcluded(true);
        updateBizOrderCommand.setVersion(command.getVersion());
        updateBizOrderCommand.setChangeId(command.getTxId());
        updateBizOrderCommand.setOrderId(command.getOrderId());
        updateOrder(command, updateBizOrderCommand, command.getTxId());
    }

    public void reservedOrder(CreateBizStateMachineCommand command) {
        UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
        updateBizOrderCommand.setOrderStorage(true);
        updateBizOrderCommand.setVersion(command.getVersion());
        updateBizOrderCommand.setChangeId(command.getTxId());
        updateBizOrderCommand.setOrderId(command.getOrderId());
        updateOrder(command, updateBizOrderCommand, command.getTxId());
    }

    public void cancelUpdateOrder(CreateBizStateMachineCommand command, String cancelTxId, String txId) {
        log.info("start of cancel created order");
        CancelUpdateBizOrderCommand cancelUpdateBizOrderCommand = new CancelUpdateBizOrderCommand();
        cancelUpdateBizOrderCommand.setCancelChangeId(txId);
        cancelUpdateBizOrderCommand.setChangeId(cancelTxId);
        cancelUpdateOrder(command, cancelUpdateBizOrderCommand, cancelTxId);
        log.info("end of cancel created order");
    }

    public void createNewOrder(String paymentLink, CreateBizStateMachineCommand command) {
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


    public void confirmPayment(CreateBizStateMachineCommand command) {
        UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
        updateBizOrderCommand.setPaid(true);
        updateBizOrderCommand.setVersion(command.getVersion());
        updateBizOrderCommand.setChangeId(command.getTxId());
        updateBizOrderCommand.setOrderId(command.getOrderId());
        updateOrder(command, updateBizOrderCommand, command.getTxId());
    }

    public void recycleOrder(CreateBizStateMachineCommand command) {
        UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
        updateBizOrderCommand.setOrderStorage(false);
        updateBizOrderCommand.setVersion(command.getVersion());
        updateBizOrderCommand.setChangeId(command.getTxId());
        updateBizOrderCommand.setOrderId(command.getOrderId());
        updateOrder(command, updateBizOrderCommand, command.getTxId());
    }

    private void updateOrder(CreateBizStateMachineCommand machineCommand, UpdateBizOrderCommand command, String changeId) {
        log.info("starting update order to {}", command);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<UpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(command, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl + "/" + machineCommand.getOrderId(), HttpMethod.PUT, hashMapHttpEntity, String.class);
        log.info("starting update order");
    }

    private void cancelUpdateOrder(CreateBizStateMachineCommand machineCommand, CancelUpdateBizOrderCommand command, String changeId) {
        log.info("starting update order to {}", command);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<CancelUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(command, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl + "/" + machineCommand.getOrderId(), HttpMethod.PUT, hashMapHttpEntity, String.class);
        log.info("starting update order");
    }
}

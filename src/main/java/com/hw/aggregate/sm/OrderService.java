package com.hw.aggregate.sm;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.model.order.AppCreateBizOrderCommand;
import com.hw.aggregate.sm.model.order.AppUpdateBizOrderCommand;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
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

    public void concludeOrder(CreateBizStateMachineCommand machineCommand) {
        updateOrder(machineCommand, AppUpdateBizOrderCommand.CommandType.CONCLUDE);
    }

    public void reservedOrder(CreateBizStateMachineCommand machineCommand) {
        updateOrder(machineCommand, AppUpdateBizOrderCommand.CommandType.RESERVE);
    }

    public void cancelReservedOrder(CreateBizStateMachineCommand machineCommand, String cancelTxId, String txId) {
        log.info("start of cancel created order");
        updateOrder(machineCommand, AppUpdateBizOrderCommand.CommandType.CANCEL_RESERVE, cancelTxId);
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

    public void cancelCreateNewOrder(CreateBizStateMachineCommand command, String cancelTxId, String txId) {
        log.info("start of cancel created order");
        updateOrder(command, AppUpdateBizOrderCommand.CommandType.CANCEL_CREATE, cancelTxId);
        log.info("end of cancel created order");
    }

    public void cancelConcludeOrder(CreateBizStateMachineCommand command, String cancelTxId, String txId) {
        log.info("start of cancel conclude order");
        updateOrder(command, AppUpdateBizOrderCommand.CommandType.CANCEL_CONCLUDE, cancelTxId);
        log.info("end of cancel conclude order");
    }

    public void cancelConfirmPayment(CreateBizStateMachineCommand command, String cancelTxId, String txId) {
        log.info("start of cancel conclude order");
        updateOrder(command, AppUpdateBizOrderCommand.CommandType.CANCEL_CONFIRM_PAYMENT, cancelTxId);
        log.info("end of cancel conclude order");
    }

    public void cancelRecycleOrder(CreateBizStateMachineCommand command, String cancelTxId, String txId) {
        log.info("start of cancel recycle order");
        updateOrder(command, AppUpdateBizOrderCommand.CommandType.CANCEL_RECYCLE, cancelTxId);
        log.info("end of cancel recycle order");
    }

    public void confirmPayment(CreateBizStateMachineCommand command) {
        updateOrder(command, AppUpdateBizOrderCommand.CommandType.CONFIRM_PAYMENT);
    }

    public void recycleOrder(CreateBizStateMachineCommand command) {
        updateOrder(command, AppUpdateBizOrderCommand.CommandType.RECYCLE);
    }

    private void updateOrder(CreateBizStateMachineCommand machineCommand, AppUpdateBizOrderCommand.CommandType commandType) {
        updateOrder(machineCommand, commandType, machineCommand.getTxId());
    }

    private void updateOrder(CreateBizStateMachineCommand machineCommand, AppUpdateBizOrderCommand.CommandType commandType, String changeId) {
        log.info("starting update order to {}", commandType);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, changeId);
        AppUpdateBizOrderCommand command = new AppUpdateBizOrderCommand();
        command.setOrderId(machineCommand.getOrderId());
        command.setCommandType(commandType);
        command.setVersion(machineCommand.getVersion());
        HttpEntity<AppUpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(command, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl + "/" + machineCommand.getOrderId(), HttpMethod.PUT, hashMapHttpEntity, String.class);
        log.info("starting update order to {}", commandType);
    }

}

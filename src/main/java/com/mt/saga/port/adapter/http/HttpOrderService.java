package com.mt.saga.port.adapter.http;

import com.mt.common.domain.model.restful.SumPagedRep;
import com.mt.common.domain.model.service_discovery.EurekaHelper;
import com.mt.saga.domain.model.order_state_machine.event.OrderOperationEvent;
import com.mt.saga.domain.model.order_state_machine.order.AppCreateBizOrderCommand;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderStatus;
import com.mt.saga.domain.model.order_state_machine.order.UpdateBizOrderCommand;
import com.mt.saga.domain.model.task.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static com.mt.common.CommonConstant.HTTP_HEADER_CHANGE_ID;
import static com.mt.common.CommonConstant.HTTP_PARAM_QUERY;


@Slf4j
@Service
public class HttpOrderService implements OrderService {

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

    @Override
    public void concludeOrder(OrderOperationEvent command) {
        UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
        updateBizOrderCommand.setActualStorage(true);
        updateBizOrderCommand.setConcluded(true);
        updateBizOrderCommand.setVersion(command.getVersion());
        updateBizOrderCommand.setChangeId(command.getTxId());
        updateBizOrderCommand.setOrderId(command.getOrderId());
        updateOrder(command, updateBizOrderCommand, command.getTxId());
    }

    @Override
    public void reservedOrder(OrderOperationEvent command) {
        UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
        updateBizOrderCommand.setOrderStorage(true);
        updateBizOrderCommand.setVersion(command.getVersion());
        updateBizOrderCommand.setChangeId(command.getTxId());
        updateBizOrderCommand.setOrderId(command.getOrderId());
        updateOrder(command, updateBizOrderCommand, command.getTxId());
    }

    @Override
    public void cancelCreateNewOrder(OrderOperationEvent command, String cancelTxId, String txId) {
        log.info("start of cancel created order");
        if (hasChange(txId)) {
            UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
            updateBizOrderCommand.setDeleted(true);
            updateBizOrderCommand.setDeletedBy("Saga");
            updateBizOrderCommand.setVersion(command.getVersion());
            updateBizOrderCommand.setChangeId(cancelTxId);
            updateBizOrderCommand.setOrderId(command.getOrderId());
            updateOrder(command, updateBizOrderCommand, cancelTxId);
        }
        log.info("end of cancel created order");
    }

    @Override
    public void cancelConcludeOrder(OrderOperationEvent command, String cancelTxId, String txId) {
        if (hasChange(txId)) {
            UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
            updateBizOrderCommand.setActualStorage(false);
            updateBizOrderCommand.setConcluded(false);
            updateBizOrderCommand.setVersion(command.getVersion() + 1);
            updateBizOrderCommand.setChangeId(cancelTxId);
            updateBizOrderCommand.setOrderId(command.getOrderId());
            updateOrder(command, updateBizOrderCommand, cancelTxId);
        }
    }

    @Override
    public void cancelReserveOrder(OrderOperationEvent command, String cancelTxId, String txId) {
        if (hasChange(txId)) {
            UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
            updateBizOrderCommand.setPaid(false);
            updateBizOrderCommand.setVersion(command.getVersion() + 1);
            updateBizOrderCommand.setChangeId(cancelTxId);
            updateBizOrderCommand.setOrderId(command.getOrderId());
            updateOrder(command, updateBizOrderCommand, cancelTxId);
        }
    }

    @Override
    public void cancelRecycleOrder(OrderOperationEvent command, String cancelTxId, String txId) {
        if (hasChange(txId)) {
            UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
            updateBizOrderCommand.setOrderStorage(true);
            updateBizOrderCommand.setVersion(command.getVersion() + 1);
            updateBizOrderCommand.setChangeId(cancelTxId);
            updateBizOrderCommand.setOrderId(command.getOrderId());
            updateOrder(command, updateBizOrderCommand, cancelTxId);
        }
    }

    @Override
    public void cancelConfirmPayment(OrderOperationEvent command, String cancelTxId, String txId) {
        if (hasChange(txId)) {
            UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
            updateBizOrderCommand.setOrderStorage(false);
            updateBizOrderCommand.setVersion(command.getVersion() + 1);
            updateBizOrderCommand.setChangeId(cancelTxId);
            updateBizOrderCommand.setOrderId(command.getOrderId());
            updateOrder(command, updateBizOrderCommand, cancelTxId);
        }
    }

    @Override
    public void createNewOrder(AppCreateBizOrderCommand command, String changeId) {
        log.info("starting saveNewOrder");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<AppCreateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(command, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl, HttpMethod.POST, hashMapHttpEntity, String.class);
        log.info("complete saveNewOrder");
    }


    @Override
    public void confirmPayment(OrderOperationEvent command) {
        UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
        updateBizOrderCommand.setPaid(true);
        updateBizOrderCommand.setVersion(command.getVersion());
        updateBizOrderCommand.setChangeId(command.getTxId());
        updateBizOrderCommand.setOrderId(command.getOrderId());
        updateOrder(command, updateBizOrderCommand, command.getTxId());
    }

    @Override
    public void recycleOrder(OrderOperationEvent command) {
        UpdateBizOrderCommand updateBizOrderCommand = new UpdateBizOrderCommand();
        updateBizOrderCommand.setOrderStorage(false);
        updateBizOrderCommand.setVersion(command.getVersion());
        updateBizOrderCommand.setChangeId(command.getTxId());
        updateBizOrderCommand.setOrderId(command.getOrderId());
        updateOrder(command, updateBizOrderCommand, command.getTxId());
    }

    private void updateOrder(OrderOperationEvent machineCommand, UpdateBizOrderCommand command, String changeId) {
        HttpOrderService.log.info("starting update order to {}", command);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<UpdateBizOrderCommand> hashMapHttpEntity = new HttpEntity<>(command, headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        restTemplate.exchange(applicationUrl + orderUrl + "/" + machineCommand.getOrderId(), HttpMethod.PUT, hashMapHttpEntity, String.class);
        HttpOrderService.log.info("starting update order");
    }

    private boolean hasChange(String changeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HTTP_HEADER_CHANGE_ID, changeId);
        HttpEntity<String> hashMapHttpEntity = new HttpEntity<>(headers);
        String applicationUrl = eurekaHelper.getApplicationUrl(appName);
        ResponseEntity<SumPagedRep> exchange = restTemplate.exchange(applicationUrl + changeUrl + "?" + HTTP_PARAM_QUERY + "=" + "entityType:BizOrderSummary,changeId:" + changeId, HttpMethod.GET, hashMapHttpEntity, SumPagedRep.class);
        return exchange.getBody().getData().size() == 1;
    }
}

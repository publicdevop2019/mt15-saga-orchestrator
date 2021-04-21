package com.hw.aggregate.tx.representation;

import com.hw.aggregate.tx.model.CreateOrderTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class AppBizTxRep {

    private String transactionId;
    private Long id;

    public AppBizTxRep(CreateOrderTask bizTask) {
        BeanUtils.copyProperties(bizTask, this);
        this.transactionId = bizTask.getTaskId();
    }

}

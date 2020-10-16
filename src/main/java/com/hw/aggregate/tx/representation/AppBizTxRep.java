package com.hw.aggregate.tx.representation;

import com.hw.aggregate.tx.model.BizTx;
import lombok.Data;

@Data
public class AppBizTxRep {

    private String transactionId;
    private Long id;

    public AppBizTxRep(BizTx bizTask) {
        this.transactionId = bizTask.getTxId();
        this.id=bizTask.getId();
    }

}

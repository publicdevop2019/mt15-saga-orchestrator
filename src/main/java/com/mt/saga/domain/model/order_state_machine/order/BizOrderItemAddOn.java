package com.mt.saga.domain.model.order_state_machine.order;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BizOrderItemAddOn implements Serializable {

    private static final long serialVersionUID = 1;

    private String title;

    private List<BizOrderItemAddOnSelection> options;

}

package com.mt.saga.domain.model.order_state_machine.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class BizOrderItemAddOnSelection implements Serializable {

    private static final long serialVersionUID = 1;

    private String optionValue;

    private String priceVar;
}

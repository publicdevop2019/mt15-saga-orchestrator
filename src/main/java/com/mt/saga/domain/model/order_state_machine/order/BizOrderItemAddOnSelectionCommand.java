package com.mt.saga.domain.model.order_state_machine.order;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BizOrderItemAddOnSelectionCommand {

    private String optionValue;

    private String priceVar;
}

package com.hw.aggregate.sm.model.order;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BizOrderItemAddOnSelectionCommand {

    private String optionValue;

    private String priceVar;
}

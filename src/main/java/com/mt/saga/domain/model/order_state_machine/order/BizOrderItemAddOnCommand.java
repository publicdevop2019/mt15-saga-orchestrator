package com.mt.saga.domain.model.order_state_machine.order;

import lombok.Data;

import java.util.List;

@Data
public class BizOrderItemAddOnCommand {

    private String title;

    private List<BizOrderItemAddOnSelectionCommand> options;

}

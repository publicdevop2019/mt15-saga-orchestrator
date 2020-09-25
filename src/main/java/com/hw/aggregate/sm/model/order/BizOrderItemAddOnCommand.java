package com.hw.aggregate.sm.model.order;

import lombok.Data;

import java.util.List;

@Data
public class BizOrderItemAddOnCommand {

    private String title;

    private List<BizOrderItemAddOnSelectionCommand> options;

}

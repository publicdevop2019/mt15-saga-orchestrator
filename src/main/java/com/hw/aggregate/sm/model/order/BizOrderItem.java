package com.hw.aggregate.sm.model.order;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class BizOrderItem implements Serializable {

    private static final long serialVersionUID = 1;

    private String name;

    private List<BizOrderItemAddOn> selectedOptions;

    private BigDecimal finalPrice;

    private Long productId;
    private Set<String> attributesSales;

    private String imageUrlSmall;

    private Map<String, String> attrIdMap;
}

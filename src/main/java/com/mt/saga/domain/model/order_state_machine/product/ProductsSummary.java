package com.mt.saga.domain.model.order_state_machine.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
public class ProductsSummary {
    protected List<ProductAdminCardRepresentation> data;
    protected Long totalItemCount;

    @Data
    public static class ProductAdminCardRepresentation {
        private String id;
        private List<AppProductOption> selectedOptions;
        private List<AppProductSku> productSkuList;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class AppProductSku {
        private Set<String> attributesSales;
        private BigDecimal price;
    }

    @Data
    public static class AppProductOption {
        public String title;
        public List<OptionItem> options;

        @Data
        @AllArgsConstructor
        public static class OptionItem {
            public String optionValue;
            public String priceVar;

            public OptionItem() {
            }
        }

    }
}

package com.mt.saga.domain.model.task;

import com.mt.common.domain.model.restful.PatchCommand;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderItemAddOn;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderItemAddOnSelection;
import com.mt.saga.domain.model.order_state_machine.order.CartDetail;
import com.mt.saga.domain.model.order_state_machine.product.ProductsSummary;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mt.common.CommonConstant.PATCH_OP_TYPE_DIFF;
import static com.mt.common.CommonConstant.PATCH_OP_TYPE_SUM;

@Slf4j
public abstract class ProductService {
    public abstract void updateProductStorage(List<PatchCommand> changeList, String txId);

    public abstract void cancelUpdateProductStorage(List<PatchCommand> originalChange, String cancelTxId, String txId);


    public abstract boolean validateOrderedProduct(List<CartDetail> customerOrderItemList);

    public List<PatchCommand> getReserveOrderPatchCommands(List<CartDetail> collect2) {
        List<PatchCommand> details = new ArrayList<>();
        collect2.forEach(e -> {
            int amount = 1;
            if (e.getSelectedOptions() != null) {
                Optional<BizOrderItemAddOn> qty = e.getSelectedOptions().stream().filter(el -> el.getTitle().equals("qty")).findFirst();
                if (qty.isPresent() && !qty.get().getOptions().isEmpty()) {
                    /**
                     * deduct amount based on qty value, otherwise default is 1
                     */
                    amount = Integer.parseInt(qty.get().getOptions().get(0).getOptionValue());
                }
            }
            PatchCommand patchCommand = new PatchCommand();
            patchCommand.setOp(PATCH_OP_TYPE_DIFF);
            patchCommand.setValue(String.valueOf(amount));
            patchCommand.setPath(getPatchPath(e, "storageOrder"));
            patchCommand.setExpect(1);
            details.add(patchCommand);
        });
        return details;
    }

    public List<PatchCommand> getConfirmOrderPatchCommands(List<CartDetail> collect2) {
        List<PatchCommand> details = new ArrayList<>();
        collect2.forEach(e -> {
            int amount = 1;
            if (e.getSelectedOptions() != null) {
                Optional<BizOrderItemAddOn> qty = e.getSelectedOptions().stream().filter(el -> el.getTitle().equals("qty")).findFirst();
                if (qty.isPresent() && !qty.get().getOptions().isEmpty()) {
                    /**
                     * deduct amount based on qty value, otherwise default is 1
                     */
                    amount = Integer.parseInt(qty.get().getOptions().get(0).getOptionValue());
                }
            }
            PatchCommand storageActualCmd = new PatchCommand();
            storageActualCmd.setOp(PATCH_OP_TYPE_DIFF);
            storageActualCmd.setValue(String.valueOf(amount));
            storageActualCmd.setPath(getPatchPath(e, "storageActual"));
            storageActualCmd.setExpect(1);
            PatchCommand salesCmd = new PatchCommand();
            salesCmd.setOp(PATCH_OP_TYPE_SUM);
            salesCmd.setValue(String.valueOf(amount));
            salesCmd.setPath(getPatchPath(e, "sales"));
            salesCmd.setExpect(1);
            details.add(storageActualCmd);
            details.add(salesCmd);
        });
        return details;
    }

    private String getPatchPath(CartDetail e, String fieldName) {
        return "/" + e.getSkuId() + "/" + fieldName;
    }

    protected boolean validateProducts(ProductsSummary productsSummary, List<CartDetail> orderItems) {
        return orderItems.stream().noneMatch(command -> {
            Optional<ProductsSummary.ProductAdminCardRepresentation> byId = productsSummary.getData().stream().filter(e -> e.getId().equals(command.getProductId())).findFirst();
            //validate product match
            if (byId.isEmpty())
                return true;
            BigDecimal price;
            if (command.getAttributesSales().isEmpty())
                command.getAttributesSales().add("");
            List<ProductsSummary.AppProductSku> collect = byId.get().getProductSkuList().stream().filter(productSku -> new TreeSet(productSku.getAttributesSales()).equals(new TreeSet(command.getAttributesSales()))).collect(Collectors.toList());
            if (collect.isEmpty())
                return true;
            price = collect.get(0).getPrice();
            //if no option present then compare final price
            if (command.getSelectedOptions() == null || command.getSelectedOptions().size() == 0) {
                return price.compareTo(command.getFinalPrice()) != 0;
            }
            //validate product option match
            List<ProductsSummary.AppProductOption> storedOption = byId.get().getSelectedOptions();
            if (storedOption == null || storedOption.size() == 0)
                return true;
            boolean optionAllMatch = command.getSelectedOptions().stream().allMatch(userSelected -> {
                //check selected option is valid option
                Optional<ProductsSummary.AppProductOption> first = storedOption.stream().filter(storedOptionItem -> {
                    // compare title
                    if (!storedOptionItem.title.equals(userSelected.getTitle()))
                        return false;
                    //compare option value for each title
                    String optionValue = userSelected.getOptions().get(0).getOptionValue();
                    Optional<ProductsSummary.AppProductOption.OptionItem> first1 = storedOptionItem.options.stream().filter(optionItem -> optionItem.getOptionValue().equals(optionValue)).findFirst();
                    return first1.isPresent();
                }).findFirst();
                return first.isPresent();
            });
            if (!optionAllMatch)
                return true;
            //validate product final price
            BigDecimal finalPrice = command.getFinalPrice();
            // get all price variable
            List<String> userSelectedAddOnTitles = command.getSelectedOptions().stream().map(BizOrderItemAddOn::getTitle).collect(Collectors.toList());
            // filter option based on title
            Stream<ProductsSummary.AppProductOption> storedAddonMatchingUserSelection = byId.get().getSelectedOptions().stream().filter(var1 -> userSelectedAddOnTitles.contains(var1.getTitle()));
            // map to value detail for each title
            List<String> priceVarCollection = storedAddonMatchingUserSelection.map(storedMatchAddon -> {
                String title = storedMatchAddon.getTitle();
                //find right option for title
                Optional<BizOrderItemAddOn> user_addon_option = command.getSelectedOptions().stream().filter(e -> e.getTitle().equals(title)).findFirst();
                BizOrderItemAddOnSelection user_optionItem = user_addon_option.get().getOptions().get(0);
                Optional<ProductsSummary.AppProductOption.OptionItem> first = storedMatchAddon.getOptions().stream().filter(db_optionItem -> db_optionItem.getOptionValue().equals(user_optionItem.getOptionValue())).findFirst();
                return first.get().getPriceVar();
            }).collect(Collectors.toList());
            for (String priceVar : priceVarCollection) {
                if (priceVar.contains("+")) {
                    double v = Double.parseDouble(priceVar.replace("+", ""));
                    BigDecimal bigDecimal = BigDecimal.valueOf(v);
                    price = price.add(bigDecimal);
                } else if (priceVar.contains("-")) {
                    double v = Double.parseDouble(priceVar.replace("-", ""));
                    BigDecimal bigDecimal = BigDecimal.valueOf(v);
                    price = price.subtract(bigDecimal);

                } else if (priceVar.contains("*")) {
                    double v = Double.parseDouble(priceVar.replace("*", ""));
                    BigDecimal bigDecimal = BigDecimal.valueOf(v);
                    price = price.multiply(bigDecimal);
                } else {
                    log.error("unknown operation type");
                }
            }
            if (price.compareTo(finalPrice) == 0) {
                log.debug("value does match for product {}, expected {} actual {}", command.getProductId(), price, finalPrice);
                return false;
            }
            return true;
        });
    }
}

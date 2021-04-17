package com.hw.aggregate.sm.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.aggregate.sm.*;
import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.exception.*;
import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import com.hw.aggregate.sm.model.order.CartDetail;
import com.hw.aggregate.tx.AppBizTxApplicationService;
import com.hw.aggregate.tx.BizTxRepository;
import com.hw.aggregate.tx.exception.BizTxPersistenceException;
import com.hw.aggregate.tx.model.CreateOrderBizTx;
import com.hw.aggregate.tx.model.SubTxStatus;
import com.hw.aggregate.tx.model.TxStatus;
import com.hw.aggregate.tx.representation.AppBizTxRep;
import com.hw.shared.IdGenerator;
import com.hw.shared.rest.CreatedAggregateRep;
import com.hw.shared.rest.exception.AggregateNotExistException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.guard.Guard;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.hw.aggregate.sm.AppBizStateMachineApplicationService.BIZ_ORDER;

/**
 * each guard is an unit of work, roll back when failure happen
 */
@Configuration
@Slf4j
public class CustomStateMachineBuilder {

    public static final String TX_TASK = "TxTask";
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductService productService;

    @Autowired
    private MessengerService messengerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AppBizStateMachineApplicationService stateMachineApplicationService;

    @Autowired
    private AppBizTxApplicationService appBizTaskApplicationService;

    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CustomStateMachineEventListener customStateMachineEventListener;

    @Autowired
    private IdGenerator idGenerator;
    @Autowired
    private BizTxRepository bizTxRepository;
    @Autowired
    private ObjectMapper objectMapper;


    public StateMachine<BizOrderStatus, BizOrderEvent> buildMachine(BizOrderStatus initialState) {
        StateMachineBuilder.Builder<BizOrderStatus, BizOrderEvent> builder = StateMachineBuilder.builder();
        try {
            builder.configureConfiguration()
                    .withConfiguration()
                    .autoStartup(true)
                    .listener(customStateMachineEventListener)
            ;
            builder.configureStates()
                    .withStates()
                    .initial(initialState)
                    .states(EnumSet.allOf(BizOrderStatus.class));
            builder.configureTransitions()
                    .withInternal()
                    .source(BizOrderStatus.DRAFT)
                    .event(BizOrderEvent.PREPARE_NEW_ORDER)
                    .action(prepareTaskFor(BizOrderEvent.NEW_ORDER))
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.DRAFT).target(BizOrderStatus.NOT_PAID_RESERVED)
                    .event(BizOrderEvent.NEW_ORDER)
                    .guard(createOrderTx())
                    .and()
                    .withInternal()
                    .source(BizOrderStatus.NOT_PAID_RESERVED)
                    .event(BizOrderEvent.PREPARE_CONFIRM_PAYMENT)
                    .action(prepareTaskFor(BizOrderEvent.CONFIRM_PAYMENT))
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.NOT_PAID_RESERVED).target(BizOrderStatus.PAID_RESERVED)
                    .event(BizOrderEvent.CONFIRM_PAYMENT)
                    .guard(updatePaymentTx())
                    .action(concludeOrderTask())
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.NOT_PAID_RECYCLED).target(BizOrderStatus.PAID_RECYCLED)
                    .event(BizOrderEvent.CONFIRM_PAYMENT)
                    .guard(updatePaymentTx())
                    .and()
                    .withInternal()
                    .source(BizOrderStatus.NOT_PAID_RECYCLED)
                    .event(BizOrderEvent.PREPARE_CONFIRM_PAYMENT)
                    .action(prepareTaskFor(BizOrderEvent.PREPARE_CONFIRM_PAYMENT))
                    .and()
                    .withInternal()
                    .source(BizOrderStatus.PAID_RECYCLED)
                    .event(BizOrderEvent.PREPARE_RESERVE)
                    .action(prepareTaskFor(BizOrderEvent.RESERVE))
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.PAID_RECYCLED).target(BizOrderStatus.PAID_RESERVED)
                    .event(BizOrderEvent.RESERVE)
                    .guard(reserveOrderTx())
                    .action(concludeOrderTask())
                    .and()
                    .withInternal()
                    .source(BizOrderStatus.NOT_PAID_RECYCLED)
                    .event(BizOrderEvent.PREPARE_RESERVE)
                    .action(prepareTaskFor(BizOrderEvent.RESERVE))
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.NOT_PAID_RECYCLED).target(BizOrderStatus.NOT_PAID_RESERVED)
                    .event(BizOrderEvent.RESERVE)
                    .guard(reserveOrderTx())
                    .and()
                    .withInternal()
                    .source(BizOrderStatus.PAID_RESERVED)
                    .event(BizOrderEvent.PREPARE_CONFIRM_ORDER)
                    .action(prepareTaskFor(BizOrderEvent.CONFIRM_ORDER))
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.PAID_RESERVED).target(BizOrderStatus.CONFIRMED)
                    .event(BizOrderEvent.CONFIRM_ORDER)
                    .guard(confirmOrderTx())
                    .action(sendNotification())
                    .and()
                    .withInternal()
                    .source(BizOrderStatus.NOT_PAID_RESERVED)
                    .event(BizOrderEvent.PREPARE_RECYCLE_ORDER_STORAGE)
                    .action(prepareTaskFor(BizOrderEvent.RECYCLE_ORDER_STORAGE))
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.NOT_PAID_RESERVED).target(BizOrderStatus.NOT_PAID_RECYCLED)
                    .event(BizOrderEvent.RECYCLE_ORDER_STORAGE)
                    .guard(recycleOrder())
            ;
        } catch (Exception e) {
            throw new StateMachineCreationException(e);
        }
        return builder.build();
    }

    private Guard<BizOrderStatus, BizOrderEvent> recycleOrder() {
        return context -> {
            CreateBizStateMachineCommand machineCommand = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            log.info("start of recycle order with id {}", machineCommand.getOrderId());
            CreatedAggregateRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedAggregateRep.class);
            AppBizTxRep appBizTaskRep = getAppBizTaskRep(context, transactionalTask);
            if (appBizTaskRep == null) return false;

            // increase order storage
            CompletableFuture<Void> increaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(productService.getReserveOrderPatchCommands(machineCommand.getProductList()), appBizTaskRep.getTransactionId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.recycleOrder(machineCommand), customExecutor
            );

            List<RuntimeException> exs = new ArrayList<>();
            try {
                increaseOrderStorageFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderStorageDecreaseException(e));
                }
            }

            try {
                updateOrderFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderUpdateException(e));
                }
            }
            log.info("end of recycle order with id {}", machineCommand.getOrderId());
            return checkResult(context, exs, appBizTaskRep);
        };
    }

    private Action<BizOrderStatus, BizOrderEvent> prepareTaskFor(BizOrderEvent event) {
        return context -> {
            log.info("start of save task to database");
            long id = idGenerator.getId();
            if (event.equals(BizOrderEvent.NEW_ORDER)) {
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    CreateBizStateMachineCommand customerOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
                                    String s = "unable to convert";
                                    try {
                                        s = objectMapper.writeValueAsString(customerOrder);
                                    } catch (JsonProcessingException e) {
                                        log.error("unable to convert object");
                                    }
                                    CreateOrderBizTx bizTx = CreateOrderBizTx.createTx(id, s, customerOrder.getTxId());
                                    bizTxRepository.save(bizTx);
                                    context.getExtendedState().getVariables().put(TX_TASK, bizTx);
                                }
                            });
                } catch (Exception e) {
                    context.getStateMachine().setStateMachineError(new BizTxPersistenceException(e));
                }
            }
            log.info("end of save task to database");
        };
    }

    private Action<BizOrderStatus, BizOrderEvent> concludeOrderTask() {
        return context -> CompletableFuture.runAsync(() -> {
                    log.info("start of auto conclude");
                    CreateBizStateMachineCommand bizOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
                    bizOrder.setBizOrderEvent(BizOrderEvent.CONFIRM_ORDER);
                    bizOrder.setPrepareEvent(BizOrderEvent.PREPARE_CONFIRM_ORDER);
                    bizOrder.setTxId(UUID.randomUUID().toString());
                    bizOrder.setOrderState(context.getTarget().getId());
                    bizOrder.setVersion(bizOrder.getVersion() + 1);// manually increase version +1 so confirm can success
                    stateMachineApplicationService.start(bizOrder);
                }
                , customExecutor
        );
    }

    private Guard<BizOrderStatus, BizOrderEvent> createOrderTx() {
        return context -> {
            CreateBizStateMachineCommand command = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreateOrderBizTx bizTx = context.getExtendedState().get(TX_TASK, CreateOrderBizTx.class);
            List<RuntimeException> exs = new ArrayList<>();
            if (bizTx == null) return false;
            log.info("start of prepareNewOrder of {}", command.getOrderId());
            // validate product info
            CompletableFuture<Boolean> validateResultFuture = CompletableFuture.supplyAsync(() ->
                    productService.validateOrderedProduct(command.getProductList()), customExecutor
            );

            // generate payment QR link
            CompletableFuture<String> paymentQRLinkFuture = CompletableFuture.supplyAsync(() ->
                    paymentService.generatePaymentLink(command.getOrderId(), bizTx.getTxId()), customExecutor
            );

            // decrease order storage
            CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(productService.getReserveOrderPatchCommands(command.getProductList()), bizTx.getTxId()), customExecutor
            );

            // clear cart
            CompletableFuture<Void> clearCartFuture = CompletableFuture.runAsync(() -> {
                        Set<String> collect = command.getProductList().stream().map(CartDetail::getCartId).collect(Collectors.toSet());
                        cartService.clearCart(command.getUserId(), collect, bizTx.getTxId());
                    }, customExecutor
            );

            String paymentLink = null;
            try {
                paymentLink = paymentQRLinkFuture.get();
                bizTx.getGeneratePaymentLinkTx().setResults(paymentLink);
                bizTx.getGeneratePaymentLinkTx().setStatus(SubTxStatus.COMPLETED);
            } catch (InterruptedException | ExecutionException e) {
                bizTx.getGeneratePaymentLinkTx().setStatus(SubTxStatus.FAILED);
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new PaymentQRLinkGenerationException(e));
                }
            }
            try {
                Boolean aBoolean = validateResultFuture.get();
                bizTx.getValidateOrderTx().setResult(aBoolean);
                if (aBoolean.compareTo(Boolean.TRUE) == 0) {
                    bizTx.getValidateOrderTx().setResult(true);
                    bizTx.getValidateOrderTx().setStatus(SubTxStatus.COMPLETED);
                } else {
                    bizTx.getValidateOrderTx().setResult(false);
                    exs.add(new BizOrderInvalidException());
                    bizTx.getValidateOrderTx().setStatus(SubTxStatus.FAILED);
                }
            } catch (InterruptedException | ExecutionException e) {
                bizTx.getValidateOrderTx().setStatus(SubTxStatus.FAILED);
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderValidationException(e));
                }
            }
            try {
                decreaseOrderStorageFuture.get();
                bizTx.setDecreaseOrderStorageTxStatus(SubTxStatus.COMPLETED);
            } catch (InterruptedException | ExecutionException e) {
                bizTx.setDecreaseOrderStorageTxStatus(SubTxStatus.FAILED);
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderStorageDecreaseException(e));
                }
            }
            try {
                clearCartFuture.get();
                bizTx.setRemoveItemsFromCartStatus(SubTxStatus.COMPLETED);
            } catch (InterruptedException | ExecutionException e) {
                bizTx.setRemoveItemsFromCartStatus(SubTxStatus.FAILED);
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new CartClearException(e));
                }
            }


            if (exs.size() > 0) {
                markTaskAsFailed(context);
                if (exs.size() > 1) {
                    context.getStateMachine().setStateMachineError(new MultipleStateMachineException(exs));
                } else {
                    context.getStateMachine().setStateMachineError(exs.get(0));
                }
                return false;
            }

            // update order
            String finalPaymentLink = paymentLink;
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.createNewOrder(finalPaymentLink, command), customExecutor
            );
            try {
                updateOrderFuture.get();
                bizTx.getCreateOrderTx().setStatus(SubTxStatus.COMPLETED);
            } catch (ExecutionException ex) {
                bizTx.getCreateOrderTx().setStatus(SubTxStatus.FAILED);
                markTaskAsFailed(context);
                if (updateOrderFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderUpdateException(ex));
                return false;
            } catch (InterruptedException e) {
                bizTx.getCreateOrderTx().setStatus(SubTxStatus.FAILED);
                markTaskAsFailed(context);
                log.warn("thread was interrupted", e);
                context.getStateMachine().setStateMachineError(e);
                Thread.currentThread().interrupt();
                return false;
            }
            markTaskAs(context, TxStatus.COMPLETED);
            return true;
        };
    }

    private void markTaskAsFailed(StateContext<BizOrderStatus, BizOrderEvent> context) {
        markTaskAs(context, TxStatus.FAILED);
    }

    private void markTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TxStatus txStatus) {
        log.info("mark task as {}", txStatus);
        CreateOrderBizTx o = (CreateOrderBizTx) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTxStatus(txStatus);
        bizTxRepository.save(o);
    }

    @Nullable
    private AppBizTxRep getAppBizTaskRep(StateContext<BizOrderStatus, BizOrderEvent> context, CreatedAggregateRep transactionalTask) {
        AppBizTxRep appBizTaskRep;
        try {
            log.info("read saved task");
            appBizTaskRep = appBizTaskApplicationService.readById(transactionalTask.getId());
        } catch (AggregateNotExistException ex) {
            context.getStateMachine().setStateMachineError(ex);
            return null;
        }
        return appBizTaskRep;
    }

    private Guard<BizOrderStatus, BizOrderEvent> reserveOrderTx() {
        return context -> {
            log.info("start of reserveOrderTx");
            CreateBizStateMachineCommand stateMachineCommand = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedAggregateRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedAggregateRep.class);
            AppBizTxRep appBizTaskRep = getAppBizTaskRep(context, transactionalTask);
            if (appBizTaskRep == null) return false;

            // decrease order storage
            CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(productService.getReserveOrderPatchCommands(stateMachineCommand.getProductList()), appBizTaskRep.getTransactionId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.saveReservedOrder(stateMachineCommand), customExecutor
            );

            List<RuntimeException> exs = new ArrayList<>();
            try {
                decreaseOrderStorageFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderStorageDecreaseException(e));
                }
            }

            try {
                updateOrderFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderUpdateException(e));
                }
            }

            return checkResult(context, exs, appBizTaskRep);
        };
    }

    private Action<BizOrderStatus, BizOrderEvent> sendNotification() {
        return context -> {
            log.info("start of sendEmailNotification");
            messengerService.notifyBusinessOwner(new HashMap<>());
        };
    }

    private Guard<BizOrderStatus, BizOrderEvent> confirmOrderTx() {
        return context -> {
            CreateBizStateMachineCommand machineCommand = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedAggregateRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedAggregateRep.class);
            AppBizTxRep appBizTaskRep = getAppBizTaskRep(context, transactionalTask);
            if (appBizTaskRep == null) return false;
            log.info("start of decreaseActualStorage for {}", machineCommand.getOrderId());
            // decrease actual storage
            CompletableFuture<Void> decreaseActualStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(productService.getConfirmOrderPatchCommands(machineCommand.getProductList()), appBizTaskRep.getTransactionId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.concludeOrder(machineCommand), customExecutor
            );

            List<RuntimeException> exs = new ArrayList<>();
            try {
                decreaseActualStorageFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new ActualStorageDecreaseException(e));
                }
            }

            try {
                updateOrderFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderUpdateException(e));
                }
            }

            return checkResult(context, exs, appBizTaskRep);
        };
    }

    private Guard<BizOrderStatus, BizOrderEvent> updatePaymentTx() {
        return context -> {
            log.info("start of updatePaymentStatus");
            CreateBizStateMachineCommand bizOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedAggregateRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedAggregateRep.class);
            AppBizTxRep appBizTaskRep = getAppBizTaskRep(context, transactionalTask);
            if (appBizTaskRep == null) return false;

            // confirm payment
            CompletableFuture<Boolean> confirmPaymentFuture = CompletableFuture.supplyAsync(() ->
                    paymentService.confirmPaymentStatus(bizOrder.getOrderId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.confirmPayment(bizOrder), customExecutor
            );

            List<RuntimeException> exs = new ArrayList<>();
            try {
                if (!Boolean.TRUE.equals(confirmPaymentFuture.get())) {
                    exs.add(new PaymentConfirmationFailedException());
                }
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new PaymentConfirmationFailedException(e));
                }
            }

            try {
                updateOrderFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderUpdateException(e));
                }
            }

            return checkResult(context, exs, appBizTaskRep);
        };
    }

    private boolean checkResult(StateContext<BizOrderStatus, BizOrderEvent> context, List<RuntimeException> exs, AppBizTxRep appBizTxRep) {
        markTaskAsFailed(context);
        if (exs.size() > 0) {
            if (exs.size() > 1) {
                context.getStateMachine().setStateMachineError(new MultipleStateMachineException(exs));
            } else {
                context.getStateMachine().setStateMachineError(exs.get(0));
            }
            return false;
        }
        return true;
    }


}

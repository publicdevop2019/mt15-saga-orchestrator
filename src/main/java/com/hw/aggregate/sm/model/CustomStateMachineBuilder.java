package com.hw.aggregate.sm.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hw.aggregate.sm.*;
import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.exception.*;
import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import com.hw.aggregate.sm.model.order.CartDetail;
import com.hw.aggregate.tx.*;
import com.hw.aggregate.tx.exception.BizTxPersistenceException;
import com.hw.aggregate.tx.model.*;
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
    private AppTaskApplicationService appBizTaskApplicationService;

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
    private CreateOrderTaskRepository createOrderTaskRepository;
    @Autowired
    private RecycleOrderTaskRepository recycleOrderTaskRepository;
    @Autowired
    private ReserveOrderTaskRepository reserveOrderTaskRepository;
    @Autowired
    private ConfirmOrderPaymentTaskRepository confirmOrderPaymentTaskRepository;
    @Autowired
    private ConcludeOrderTaskRepository concludeOrderTaskRepository;
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
                    .guard(confirmPaymentTx())
                    .action(concludeOrderTask())
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.NOT_PAID_RECYCLED).target(BizOrderStatus.PAID_RECYCLED)
                    .event(BizOrderEvent.CONFIRM_PAYMENT)
                    .guard(confirmPaymentTx())
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
            RecycleOrderTask task = context.getExtendedState().get(TX_TASK, RecycleOrderTask.class);
            // increase order storage
            CompletableFuture<Void> increaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(productService.getReserveOrderPatchCommands(machineCommand.getProductList()), task.getTaskId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.recycleOrder(machineCommand), customExecutor
            );

            List<RuntimeException> exs = new ArrayList<>();
            try {
                increaseOrderStorageFuture.get();
                task.setIncreaseOrderStorageSubTaskStatus(SubTaskStatus.COMPLETED);
            } catch (InterruptedException | ExecutionException e) {
                log.error("error during recycle order storage", e);
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
                task.setUpdateOrderSubTaskStatus(SubTaskStatus.COMPLETED);
            } catch (InterruptedException | ExecutionException e) {
                log.error("error during update order status", e);
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderUpdateException(e));
                }
            }
            log.info("end of recycle order with id {}", machineCommand.getOrderId());
            if (exs.size() > 0) {
                markRecycleTaskAs(context, TaskStatus.FAILED);
                if (exs.size() > 1) {
                    context.getStateMachine().setStateMachineError(new MultipleStateMachineException(exs));
                } else {
                    context.getStateMachine().setStateMachineError(exs.get(0));
                }
                return false;
            }
            markRecycleTaskAs(context, TaskStatus.COMPLETED);
            return true;
        };
    }

    private Action<BizOrderStatus, BizOrderEvent> prepareTaskFor(BizOrderEvent event) {
        return context -> {
            long id = idGenerator.getId();
            if (event.equals(BizOrderEvent.NEW_ORDER)) {
                log.info("start of save create order task");
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock

                                    CreateBizStateMachineCommand customerOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
                                    CreateOrderTask bizTx = CreateOrderTask.createTask(id, getCommandAsString(context), customerOrder.getTxId());
                                    createOrderTaskRepository.save(bizTx);
                                    context.getExtendedState().getVariables().put(TX_TASK, bizTx);
                                }
                            });
                } catch (Exception e) {
                    context.getStateMachine().setStateMachineError(new BizTxPersistenceException(e));
                }
                log.info("end of save create order task");
            } else if (event.equals(BizOrderEvent.RECYCLE_ORDER_STORAGE)) {
                log.info("start of save recycle order task");
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    CreateBizStateMachineCommand customerOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
                                    RecycleOrderTask task = RecycleOrderTask.createTask(id, getCommandAsString(context), customerOrder.getTxId());
                                    recycleOrderTaskRepository.save(task);
                                    context.getExtendedState().getVariables().put(TX_TASK, task);
                                }
                            });
                } catch (Exception e) {
                    context.getStateMachine().setStateMachineError(new BizTxPersistenceException(e));
                }
                log.info("end of save recycle order task");
            } else if (event.equals(BizOrderEvent.RESERVE)) {
                log.info("start of save reserve order task");
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    CreateBizStateMachineCommand customerOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
                                    ReserveOrderTask task = ReserveOrderTask.createTask(id, getCommandAsString(context), customerOrder.getTxId());
                                    reserveOrderTaskRepository.save(task);
                                    context.getExtendedState().getVariables().put(TX_TASK, task);
                                }
                            });
                } catch (Exception e) {
                    context.getStateMachine().setStateMachineError(new BizTxPersistenceException(e));
                }
                log.info("end of save reserve order task");
            } else if (event.equals(BizOrderEvent.CONFIRM_PAYMENT)) {
                log.info("start of save confirm payment order task");
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    CreateBizStateMachineCommand customerOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
                                    ConfirmOrderPaymentTask task = ConfirmOrderPaymentTask.createTask(id, getCommandAsString(context), customerOrder.getTxId());
                                    confirmOrderPaymentTaskRepository.save(task);
                                    context.getExtendedState().getVariables().put(TX_TASK, task);
                                }
                            });
                } catch (Exception e) {
                    context.getStateMachine().setStateMachineError(new BizTxPersistenceException(e));
                }
                log.info("end of save confirm payment order task");
            } else if (event.equals(BizOrderEvent.CONFIRM_ORDER)) {
                log.info("start of save confirm payment order task");
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    CreateBizStateMachineCommand customerOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
                                    ConcludeOrderTask task = ConcludeOrderTask.createTask(id, getCommandAsString(context), customerOrder.getTxId());
                                    concludeOrderTaskRepository.save(task);
                                    context.getExtendedState().getVariables().put(TX_TASK, task);
                                }
                            });
                } catch (Exception e) {
                    context.getStateMachine().setStateMachineError(new BizTxPersistenceException(e));
                }
                log.info("end of save confirm payment order task");
            }
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
            CreateOrderTask bizTx = context.getExtendedState().get(TX_TASK, CreateOrderTask.class);
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
                bizTx.getGeneratePaymentLinkTx().setStatus(SubTaskStatus.COMPLETED);
            } catch (InterruptedException | ExecutionException e) {
                bizTx.getGeneratePaymentLinkTx().setStatus(SubTaskStatus.FAILED);
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
                    bizTx.getValidateOrderTx().setStatus(SubTaskStatus.COMPLETED);
                } else {
                    bizTx.getValidateOrderTx().setResult(false);
                    exs.add(new BizOrderInvalidException());
                    bizTx.getValidateOrderTx().setStatus(SubTaskStatus.FAILED);
                }
            } catch (InterruptedException | ExecutionException e) {
                bizTx.getValidateOrderTx().setStatus(SubTaskStatus.FAILED);
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
                bizTx.setDecreaseOrderStorageTxStatus(SubTaskStatus.COMPLETED);
            } catch (InterruptedException | ExecutionException e) {
                bizTx.setDecreaseOrderStorageTxStatus(SubTaskStatus.FAILED);
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
                bizTx.setRemoveItemsFromCartStatus(SubTaskStatus.COMPLETED);
            } catch (InterruptedException | ExecutionException e) {
                bizTx.setRemoveItemsFromCartStatus(SubTaskStatus.FAILED);
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new CartClearException(e));
                }
            }


            if (exs.size() > 0) {
                markCreateTaskAs(context, TaskStatus.FAILED);
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
                bizTx.getCreateOrderTx().setStatus(SubTaskStatus.COMPLETED);
            } catch (ExecutionException ex) {
                bizTx.getCreateOrderTx().setStatus(SubTaskStatus.FAILED);
                markCreateTaskAs(context, TaskStatus.FAILED);
                if (updateOrderFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderUpdateException(ex));
                return false;
            } catch (InterruptedException e) {
                bizTx.getCreateOrderTx().setStatus(SubTaskStatus.FAILED);
                markCreateTaskAs(context, TaskStatus.FAILED);
                log.warn("thread was interrupted", e);
                context.getStateMachine().setStateMachineError(e);
                Thread.currentThread().interrupt();
                return false;
            }
            markCreateTaskAs(context, TaskStatus.COMPLETED);
            return true;
        };
    }

    private Guard<BizOrderStatus, BizOrderEvent> reserveOrderTx() {
        return context -> {
            log.info("start of reserveOrderTx");
            CreateBizStateMachineCommand stateMachineCommand = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            ReserveOrderTask task = context.getExtendedState().get(TX_TASK, ReserveOrderTask.class);

            // decrease order storage
            CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(productService.getReserveOrderPatchCommands(stateMachineCommand.getProductList()), task.getTaskId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.reservedOrder(stateMachineCommand), customExecutor
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
            if (exs.size() > 0) {
                if (exs.size() > 1) {
                    context.getStateMachine().setStateMachineError(new MultipleStateMachineException(exs));
                } else {
                    context.getStateMachine().setStateMachineError(exs.get(0));
                }
                markReserveTaskAs(context, TaskStatus.FAILED);
                return false;
            }
            markReserveTaskAs(context, TaskStatus.COMPLETED);
            return true;
        };
    }

    private Action<BizOrderStatus, BizOrderEvent> sendNotification() {
        return context -> {
            log.info("start of sendEmailNotification");
            messengerService.notifyBusinessOwner(new HashMap<>());
        };
    }

    private String getCommandAsString(StateContext<BizOrderStatus, BizOrderEvent> context) {
        CreateBizStateMachineCommand customerOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
        String s = "unable to convert";
        try {
            s = objectMapper.writeValueAsString(customerOrder);
        } catch (JsonProcessingException e) {
            log.error("unable to convert object");
        }
        return s;
    }

    private Guard<BizOrderStatus, BizOrderEvent> confirmOrderTx() {
        return context -> {
            CreateBizStateMachineCommand machineCommand = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            ConcludeOrderTask task = context.getExtendedState().get(TX_TASK, ConcludeOrderTask.class);
            log.info("start of decreaseActualStorage for {}", machineCommand.getOrderId());
            // decrease actual storage
            CompletableFuture<Void> decreaseActualStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(productService.getConfirmOrderPatchCommands(machineCommand.getProductList()), task.getTaskId()), customExecutor
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
            if (exs.size() > 0) {
                if (exs.size() > 1) {
                    context.getStateMachine().setStateMachineError(new MultipleStateMachineException(exs));
                } else {
                    context.getStateMachine().setStateMachineError(exs.get(0));
                }
                markConcludeOrderTaskAs(context,TaskStatus.FAILED);
                return false;
            }
            markConcludeOrderTaskAs(context,TaskStatus.COMPLETED);
            return true;
        };
    }

    private Guard<BizOrderStatus, BizOrderEvent> confirmPaymentTx() {
        return context -> {
            log.info("start of updatePaymentStatus");
            CreateBizStateMachineCommand bizOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            ConfirmOrderPaymentTask task = context.getExtendedState().get(TX_TASK, ConfirmOrderPaymentTask.class);

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
                task.setUpdateOrderSubTaskStatus(SubTaskStatus.COMPLETED);
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new BizOrderUpdateException(e));
                }
            }
            if (exs.size() > 0) {
                if (exs.size() > 1) {
                    context.getStateMachine().setStateMachineError(new MultipleStateMachineException(exs));
                } else {
                    context.getStateMachine().setStateMachineError(exs.get(0));
                }
                markConfirmPaymentTaskAs(context, TaskStatus.FAILED);
                return false;
            }
            markConfirmPaymentTaskAs(context, TaskStatus.COMPLETED);
            return true;
        };
    }


    private void markCreateTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TaskStatus txStatus) {
        log.info("mark task as {}", txStatus);
        CreateOrderTask o = (CreateOrderTask) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTxStatus(txStatus);
        createOrderTaskRepository.save(o);
    }

    private void markRecycleTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TaskStatus txStatus) {
        log.info("mark task as {}", txStatus);
        RecycleOrderTask o = (RecycleOrderTask) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTaskStatus(txStatus);
        recycleOrderTaskRepository.save(o);
    }

    private void markReserveTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TaskStatus txStatus) {
        log.info("mark task as {}", txStatus);
        ReserveOrderTask o = (ReserveOrderTask) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTaskStatus(txStatus);
        reserveOrderTaskRepository.save(o);
    }

    private void markConfirmPaymentTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TaskStatus txStatus) {
        log.info("mark task as {}", txStatus);
        ConfirmOrderPaymentTask o = (ConfirmOrderPaymentTask) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTaskStatus(txStatus);
        confirmOrderPaymentTaskRepository.save(o);
    }
    private void markConcludeOrderTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TaskStatus txStatus) {
        log.info("mark task as {}", txStatus);
        ConcludeOrderTask o = (ConcludeOrderTask) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTaskStatus(txStatus);
        concludeOrderTaskRepository.save(o);
    }

}

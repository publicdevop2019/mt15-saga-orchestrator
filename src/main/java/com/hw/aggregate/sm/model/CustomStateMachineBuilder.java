package com.hw.aggregate.sm.model;

import com.hw.aggregate.sm.*;
import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.exception.*;
import com.hw.aggregate.sm.model.order.BizOrderEvent;
import com.hw.aggregate.sm.model.order.BizOrderStatus;
import com.hw.aggregate.tx.AppBizTxApplicationService;
import com.hw.aggregate.tx.command.AppCreateBizTxCommand;
import com.hw.aggregate.tx.command.AppUpdateBizTxCommand;
import com.hw.aggregate.tx.exception.BizTxPersistenceException;
import com.hw.aggregate.tx.model.BizTxStatus;
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

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    ;

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
            log.info("start of recycle order");
            CreateBizStateMachineCommand machineCommand = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedAggregateRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedAggregateRep.class);
            AppBizTxRep appBizTaskRep = getAppBizTaskRep(context, transactionalTask);
            if (appBizTaskRep == null) return false;

            // increase order storage
            CompletableFuture<Void> increaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(machineCommand.getOrderStorageChange(), appBizTaskRep.getTransactionId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.saveRecycleOrder(machineCommand, context.getTarget().getId()), customExecutor
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

            return checkResult(context, exs, appBizTaskRep);
        };
    }

    private Action<BizOrderStatus, BizOrderEvent> prepareTaskFor(BizOrderEvent event) {
        return context -> {
            log.info("start of save task to database");
            CreateBizStateMachineCommand customerOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            AppCreateBizTxCommand appCreateBizTaskCommand = new AppCreateBizTxCommand();
            appCreateBizTaskCommand.setReferenceId(customerOrder.getOrderId());
            appCreateBizTaskCommand.setTaskName(event);
            appCreateBizTaskCommand.setTransactionId(customerOrder.getTxId());
            CreatedAggregateRep createdEntityRep = null;
            try {
                createdEntityRep = appBizTaskApplicationService.create(appCreateBizTaskCommand, UUID.randomUUID().toString());// for create task, use random uuid
            } catch (Exception e) {
                context.getStateMachine().setStateMachineError(new BizTxPersistenceException(e));
            }
            context.getExtendedState().getVariables().put(TX_TASK, createdEntityRep);
            log.info("start of save task to database");
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
            CreateBizStateMachineCommand stateMachineCommand = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedAggregateRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedAggregateRep.class);
            List<RuntimeException> exs = new ArrayList<>();
            AppBizTxRep appBizTxRep = getAppBizTaskRep(context, transactionalTask);
            if (appBizTxRep == null) return false;
            log.info("start of prepareNewOrder of {}", stateMachineCommand.getOrderId());
            // validate product info
            CompletableFuture<Void> validateResultFuture = CompletableFuture.runAsync(() ->
                    orderService.validateOrder(stateMachineCommand.getProductList()), customExecutor
            );

            // generate payment QR link
            CompletableFuture<String> paymentQRLinkFuture = CompletableFuture.supplyAsync(() ->
                    paymentService.generatePaymentLink(stateMachineCommand.getOrderId(), appBizTxRep.getTransactionId()), customExecutor
            );

            // decrease order storage
            CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(stateMachineCommand.getOrderStorageChange(), appBizTxRep.getTransactionId()), customExecutor
            );

            // clear cart
            CompletableFuture<Void> clearCartFuture = CompletableFuture.runAsync(() ->
                    cartService.clearCart(stateMachineCommand.getUserId(), appBizTxRep.getTransactionId()), customExecutor
            );

            String paymentLink = null;
            try {
                paymentLink = paymentQRLinkFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new PaymentQRLinkGenerationException(e));
                }
            }
            try {
                validateResultFuture.get();
            } catch (InterruptedException | ExecutionException e) {
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
                clearCartFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    log.warn("thread was interrupted", e);
                    context.getStateMachine().setStateMachineError(e);
                    Thread.currentThread().interrupt();
                } else {
                    exs.add(new CartClearException(e));
                }
            }


            if (exs.size() > 0) {
                AppUpdateBizTxCommand appUpdateBizTaskCommand = new AppUpdateBizTxCommand();
                appUpdateBizTaskCommand.setTaskStatus(BizTxStatus.FAIL);
                appBizTaskApplicationService.replaceById(appBizTxRep.getId(), appUpdateBizTaskCommand, appBizTxRep.getTransactionId());
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
                    orderService.saveNewOrder(finalPaymentLink, context.getTarget().getId(), stateMachineCommand), customExecutor
            );
            try {
                updateOrderFuture.get();
            } catch (ExecutionException ex) {
                AppUpdateBizTxCommand appUpdateBizTaskCommand = new AppUpdateBizTxCommand();
                appUpdateBizTaskCommand.setTaskStatus(BizTxStatus.FAIL);
                appBizTaskApplicationService.replaceById(appBizTxRep.getId(), appUpdateBizTaskCommand, appBizTxRep.getTransactionId());
                if (updateOrderFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderUpdateException(ex));
                return false;
            } catch (InterruptedException e) {
                AppUpdateBizTxCommand appUpdateBizTaskCommand = new AppUpdateBizTxCommand();
                appUpdateBizTaskCommand.setTaskStatus(BizTxStatus.FAIL);
                appBizTaskApplicationService.replaceById(appBizTxRep.getId(), appUpdateBizTaskCommand, appBizTxRep.getTransactionId());
                log.warn("thread was interrupted", e);
                context.getStateMachine().setStateMachineError(e);
                Thread.currentThread().interrupt();
                return false;
            }
            AppUpdateBizTxCommand appUpdateBizTaskCommand = new AppUpdateBizTxCommand();
            appUpdateBizTaskCommand.setTaskStatus(BizTxStatus.COMPLETED);
            appBizTaskApplicationService.replaceById(appBizTxRep.getId(), appUpdateBizTaskCommand, appBizTxRep.getTransactionId());
            return true;
        };
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
                    productService.updateProductStorage(stateMachineCommand.getOrderStorageChange(), appBizTaskRep.getTransactionId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.saveReservedOrder(stateMachineCommand, context.getTarget().getId()), customExecutor
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
                    productService.updateProductStorage(machineCommand.getActualStorageChange(), appBizTaskRep.getTransactionId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.saveConcludeOrder(machineCommand, context.getTarget().getId()), customExecutor
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
                    orderService.savePaidOrder(bizOrder, context.getTarget().getId()), customExecutor
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
        AppUpdateBizTxCommand appUpdateBizTaskCommand = new AppUpdateBizTxCommand();
        appUpdateBizTaskCommand.setTaskStatus(exs.size() == 0 ? BizTxStatus.COMPLETED : BizTxStatus.FAIL);
        appBizTaskApplicationService.replaceById(appBizTxRep.getId(), appUpdateBizTaskCommand, appBizTxRep.getTransactionId());
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

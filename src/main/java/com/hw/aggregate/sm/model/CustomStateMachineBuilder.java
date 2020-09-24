package com.hw.aggregate.sm.model;

import com.hw.aggregate.sm.*;
import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import com.hw.aggregate.sm.exception.*;
import com.hw.aggregate.task.AppBizTaskApplicationService;
import com.hw.aggregate.task.command.AppCreateBizTaskCommand;
import com.hw.aggregate.task.command.AppUpdateBizTaskCommand;
import com.hw.aggregate.task.model.BizTaskStatus;
import com.hw.aggregate.task.representation.AppBizTaskRep;
import com.hw.shared.IdGenerator;
import com.hw.shared.rest.CreatedEntityRep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.guard.Guard;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.hw.aggregate.sm.BizStateMachineApplicationService.BIZ_ORDER;

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
    private BizStateMachineApplicationService stateMachineApplicationService;
    ;

    @Autowired
    private AppBizTaskApplicationService appBizTaskApplicationService;

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
                    .guard(createOrderTask())
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.NOT_PAID_RESERVED).target(BizOrderStatus.PAID_RESERVED)
                    .event(BizOrderEvent.CONFIRM_PAYMENT)
                    .guard(checkPaymentTask())
                    .action(concludeOrderTask())
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.NOT_PAID_RECYCLED).target(BizOrderStatus.PAID_RECYCLED)
                    .event(BizOrderEvent.CONFIRM_PAYMENT)
                    .guard(checkPaymentTask())
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
                    .guard(reserveOrderTask())
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
                    .guard(reserveOrderTask())
                    .and()
                    .withInternal()
                    .source(BizOrderStatus.PAID_RESERVED)
                    .event(BizOrderEvent.PREPARE_CONFIRM_ORDER)
                    .action(prepareTaskFor(BizOrderEvent.CONFIRM_ORDER))
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.PAID_RESERVED).target(BizOrderStatus.CONFIRMED)
                    .event(BizOrderEvent.CONFIRM_ORDER)
                    .guard(confirmOrderTask())
                    .action(sendNotification())
                    .and()
                    .withInternal()
                    .source(BizOrderStatus.NOT_PAID_RESERVED)
                    .event(BizOrderEvent.RECYCLE_ORDER_STORAGE)
                    .action(prepareTaskFor(BizOrderEvent.PREPARE_RECYCLE_ORDER_STORAGE))
                    .and()
                    .withExternal()
                    .source(BizOrderStatus.NOT_PAID_RESERVED).target(BizOrderStatus.NOT_PAID_RECYCLED)
                    .event(BizOrderEvent.RECYCLE_ORDER_STORAGE)
                    .guard(recycleOrder())
            ;
        } catch (Exception e) {
            log.error("error during creating state machine");
            throw new StateMachineCreationException();
        }
        return builder.build();
    }

    private Guard<BizOrderStatus, BizOrderEvent> recycleOrder() {
        return context -> {
            log.info("start of recycle order");
            CreateBizStateMachineCommand bizOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedEntityRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedEntityRep.class);
            AppBizTaskRep appBizTaskRep = appBizTaskApplicationService.readById(transactionalTask.getId());

            // decrease order storage
            CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(bizOrder.getOrderStorageChange(), appBizTaskRep.getTransactionId()), customExecutor
            );

            // update task
            CompletableFuture<Void> updateTaskFuture = CompletableFuture.runAsync(() ->
                    {
                        AppUpdateBizTaskCommand appUpdateBizTaskCommand = new AppUpdateBizTaskCommand();
                        appUpdateBizTaskCommand.setTaskStatus(BizTaskStatus.COMPLETED);
                        appBizTaskApplicationService.replaceById(transactionalTask.getId(), appUpdateBizTaskCommand, appBizTaskRep.getTransactionId());
                    }, customExecutor
            );
            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.updateOrder(null, context.getTarget().getId(), false, appBizTaskRep.getTransactionId()), customExecutor
            );
            CompletableFuture<Void> allDoneFuture2 = CompletableFuture.allOf(decreaseOrderStorageFuture, updateTaskFuture, updateOrderFuture);
            try {
                allDoneFuture2.get();
            } catch (ExecutionException ex) {
                log.error("error during prepare order async call", ex);
                if (decreaseOrderStorageFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderStorageDecreaseException());
                if (updateTaskFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new TaskPersistenceException());
                if (updateOrderFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderPersistenceException());
                return false;
            } catch (InterruptedException e) {
                log.warn("thread was interrupted", e);
                context.getStateMachine().setStateMachineError(e);
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        };
    }

    private Action<BizOrderStatus, BizOrderEvent> prepareTaskFor(BizOrderEvent event) {
        return context -> {
            log.info("start of save task to database");
            try {
                String txId = TransactionIdGenerator.getTxId();
                CreateBizStateMachineCommand customerOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
                AppCreateBizTaskCommand appCreateBizTaskCommand = new AppCreateBizTaskCommand();
                appCreateBizTaskCommand.setReferenceId(customerOrder.getOrderId());
                appCreateBizTaskCommand.setTaskName(event);
                appCreateBizTaskCommand.setTransactionId(txId);
                CreatedEntityRep createdEntityRep = appBizTaskApplicationService.create(appCreateBizTaskCommand, UUID.randomUUID().toString());
                context.getExtendedState().getVariables().put(TX_TASK, createdEntityRep);
            } catch (Exception ex) {
                log.error("error during data persist", ex);
                context.getStateMachine().setStateMachineError(new TaskPersistenceException());
            }
        };
    }

    private Action<BizOrderStatus, BizOrderEvent> concludeOrderTask() {
        return context -> CompletableFuture.runAsync(() -> {
                    log.info("start of autoConfirm");
                    CreateBizStateMachineCommand bizOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
                    bizOrder.setBizOrderEvent(BizOrderEvent.CONFIRM_ORDER);
                    bizOrder.setOrderState(BizOrderStatus.PAID_RESERVED);
                    stateMachineApplicationService.start(bizOrder);
                }
                , customExecutor
        );
    }

    private Guard<BizOrderStatus, BizOrderEvent> createOrderTask() {
        return context -> {
            CreateBizStateMachineCommand bizOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedEntityRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedEntityRep.class);
            AppBizTaskRep appBizTaskRep = appBizTaskApplicationService.readById(transactionalTask.getId());
            log.info("start of prepareNewOrder of {}", bizOrder.getOrderId());
            // validate order product info
            CompletableFuture<Void> validateResultFuture = CompletableFuture.runAsync(() ->
                    orderService.validateProduct(bizOrder.getOrderId()), customExecutor
            );

            // generate payment QR link
            CompletableFuture<String> paymentQRLinkFuture = CompletableFuture.supplyAsync(() ->
                    paymentService.generatePaymentLink(bizOrder.getOrderId(), appBizTaskRep.getTransactionId()), customExecutor
            );

            // decrease order storage
            CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(bizOrder.getOrderStorageChange(), appBizTaskRep.getTransactionId()), customExecutor
            );

            // clear cart
            CompletableFuture<Void> clearCartFuture = CompletableFuture.runAsync(() ->
                    cartService.clearCart(bizOrder.getOrderId(), bizOrder.getUserId(), transactionalTask.getId()), customExecutor
            );

            // update task
            CompletableFuture<Void> updateTaskFuture = CompletableFuture.runAsync(() ->
                    {
                        AppUpdateBizTaskCommand appUpdateBizTaskCommand = new AppUpdateBizTaskCommand();
                        appUpdateBizTaskCommand.setTaskStatus(BizTaskStatus.COMPLETED);
                        appBizTaskApplicationService.replaceById(transactionalTask.getId(), appUpdateBizTaskCommand, appBizTaskRep.getTransactionId());
                    }, customExecutor
            );

            CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(validateResultFuture, paymentQRLinkFuture, decreaseOrderStorageFuture, clearCartFuture, updateTaskFuture);
            String paymentLink;
            try {
                paymentLink = paymentQRLinkFuture.get();
                allDoneFuture.get();
            } catch (ExecutionException ex) {
                log.error("error during prepare order async call", ex);
                if (paymentQRLinkFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new PaymentQRLinkGenerationException());
                if (decreaseOrderStorageFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderStorageDecreaseException());
                if (validateResultFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new ProductInfoValidationException());
                if (clearCartFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new CartClearException());
                if (updateTaskFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new TaskPersistenceException());
                return false;
            } catch (InterruptedException e) {
                log.warn("thread was interrupted", e);
                context.getStateMachine().setStateMachineError(e);
                Thread.currentThread().interrupt();
                return false;
            }
            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.updateOrder(paymentLink, context.getTarget().getId(), false, appBizTaskRep.getTransactionId()), customExecutor
            );
            CompletableFuture<Void> allDoneFuture2 = CompletableFuture.allOf(updateOrderFuture);
            try {
                allDoneFuture2.get();
            } catch (ExecutionException ex) {
                log.error("error during prepare order async call", ex);
                if (updateOrderFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderPersistenceException());
                return false;
            } catch (InterruptedException e) {
                log.warn("thread was interrupted", e);
                context.getStateMachine().setStateMachineError(e);
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        };
    }

    private Guard<BizOrderStatus, BizOrderEvent> reserveOrderTask() {
        return context -> {
            log.info("start of decreaseOrderStorage");
            CreateBizStateMachineCommand bizOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedEntityRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedEntityRep.class);
            AppBizTaskRep appBizTaskRep = appBizTaskApplicationService.readById(transactionalTask.getId());

            // decrease order storage
            CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(bizOrder.getOrderStorageChange(), appBizTaskRep.getTransactionId()), customExecutor
            );

            // update task
            CompletableFuture<Void> updateTaskFuture = CompletableFuture.runAsync(() ->
                    {
                        AppUpdateBizTaskCommand appUpdateBizTaskCommand = new AppUpdateBizTaskCommand();
                        appUpdateBizTaskCommand.setTaskStatus(BizTaskStatus.COMPLETED);
                        appBizTaskApplicationService.replaceById(transactionalTask.getId(), appUpdateBizTaskCommand, appBizTaskRep.getTransactionId());
                    }, customExecutor
            );
            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.updateOrder(null, context.getTarget().getId(), false, appBizTaskRep.getTransactionId()), customExecutor
            );
            CompletableFuture<Void> allDoneFuture2 = CompletableFuture.allOf(decreaseOrderStorageFuture, updateTaskFuture, updateOrderFuture);
            try {
                allDoneFuture2.get();
            } catch (ExecutionException ex) {
                log.error("error during prepare order async call", ex);
                if (decreaseOrderStorageFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderStorageDecreaseException());
                if (updateTaskFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new TaskPersistenceException());
                if (updateOrderFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderPersistenceException());
                return false;
            } catch (InterruptedException e) {
                log.warn("thread was interrupted", e);
                context.getStateMachine().setStateMachineError(e);
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        };
    }

    private Action<BizOrderStatus, BizOrderEvent> sendNotification() {
        return context -> {
            log.info("start of sendEmailNotification");
            messengerService.notifyBusinessOwner(new HashMap<>());
        };
    }

    private Guard<BizOrderStatus, BizOrderEvent> confirmOrderTask() {
        return context -> {
            CreateBizStateMachineCommand bizOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedEntityRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedEntityRep.class);
            AppBizTaskRep appBizTaskRep = appBizTaskApplicationService.readById(transactionalTask.getId());
            log.info("start of decreaseActualStorage for {}", bizOrder.getOrderId());
            // decrease order storage
            CompletableFuture<Void> decreaseActualStorageFuture = CompletableFuture.runAsync(() ->
                    productService.updateProductStorage(bizOrder.getActualStorageChange(), appBizTaskRep.getTransactionId()), customExecutor
            );

            // update task
            CompletableFuture<Void> updateTaskFuture = CompletableFuture.runAsync(() ->
                    {
                        AppUpdateBizTaskCommand appUpdateBizTaskCommand = new AppUpdateBizTaskCommand();
                        appUpdateBizTaskCommand.setTaskStatus(BizTaskStatus.COMPLETED);
                        appBizTaskApplicationService.replaceById(transactionalTask.getId(), appUpdateBizTaskCommand, appBizTaskRep.getTransactionId());
                    }, customExecutor
            );
            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.updateOrder(null, context.getTarget().getId(), false, appBizTaskRep.getTransactionId()), customExecutor
            );
            CompletableFuture<Void> allDoneFuture2 = CompletableFuture.allOf(decreaseActualStorageFuture, updateTaskFuture, updateOrderFuture);
            try {
                allDoneFuture2.get();
            } catch (ExecutionException ex) {
                log.error("error during prepare order async call", ex);
                if (decreaseActualStorageFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new ActualStorageDecreaseException());
                if (updateTaskFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new TaskPersistenceException());
                if (updateOrderFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderPersistenceException());
                return false;
            } catch (InterruptedException e) {
                log.warn("thread was interrupted", e);
                context.getStateMachine().setStateMachineError(e);
                Thread.currentThread().interrupt();
                return false;
            }
            log.info("confirmOrderTask success");
            return true;
        };
    }

    private Guard<BizOrderStatus, BizOrderEvent> checkPaymentTask() {
        return context -> {
            log.info("start of updatePaymentStatus");
            CreateBizStateMachineCommand bizOrder = context.getExtendedState().get(BIZ_ORDER, CreateBizStateMachineCommand.class);
            CreatedEntityRep transactionalTask = context.getExtendedState().get(TX_TASK, CreatedEntityRep.class);
            AppBizTaskRep appBizTaskRep = appBizTaskApplicationService.readById(transactionalTask.getId());

            // confirm payment
            CompletableFuture<Boolean> confirmPaymentFuture = CompletableFuture.supplyAsync(() ->
                    paymentService.confirmPaymentStatus(bizOrder.getOrderId()), customExecutor
            );

            // update task
            CompletableFuture<Void> updateTaskFuture = CompletableFuture.runAsync(() ->
                    {
                        AppUpdateBizTaskCommand appUpdateBizTaskCommand = new AppUpdateBizTaskCommand();
                        appUpdateBizTaskCommand.setTaskStatus(BizTaskStatus.COMPLETED);
                        appBizTaskApplicationService.replaceById(transactionalTask.getId(), appUpdateBizTaskCommand, appBizTaskRep.getTransactionId());
                    }, customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    orderService.updateOrder(null, context.getTarget().getId(), true, appBizTaskRep.getTransactionId()), customExecutor
            );

            CompletableFuture<Void> allDoneFuture2 = CompletableFuture.allOf(confirmPaymentFuture, updateTaskFuture, updateOrderFuture);

            try {
                allDoneFuture2.get();
                if (!Boolean.TRUE.equals(confirmPaymentFuture.get())) {
                    context.getStateMachine().setStateMachineError(new PaymentConfirmationFailedException());
                    return false;
                }
            } catch (ExecutionException ex) {
                log.error("error during prepare order async call", ex);
                if (confirmPaymentFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new PaymentConfirmationFailedException());
                if (updateTaskFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new TaskPersistenceException());
                if (updateOrderFuture.isCompletedExceptionally())
                    context.getStateMachine().setStateMachineError(new BizOrderPersistenceException());
                return false;
            } catch (InterruptedException e) {
                log.warn("thread was interrupted", e);
                context.getStateMachine().setStateMachineError(e);
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        };
    }


}

package com.mt.saga.infrastructure;

import com.mt.common.domain.model.domain_event.DomainEventPublisher;
import com.mt.common.domain.model.restful.PatchCommand;
import com.mt.saga.appliction.ApplicationServiceRegistry;
import com.mt.saga.appliction.order_state_machine.OrderStateMachineApplicationService;
import com.mt.saga.domain.DomainRegistry;
import com.mt.saga.domain.model.order_state_machine.OrderStateMachineBuilder;
import com.mt.saga.domain.model.order_state_machine.event.UserPlaceOrderEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.forward.ClearCartEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.forward.DecreaseOrderStorageEvent;
import com.mt.saga.domain.model.order_state_machine.event.create_new_order.forward.GeneratePaymentQRLinkEvent;
import com.mt.saga.domain.model.order_state_machine.exception.*;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderEvent;
import com.mt.saga.domain.model.order_state_machine.order.BizOrderStatus;
import com.mt.saga.domain.model.order_state_machine.order.CartDetail;
import com.mt.saga.domain.model.task.BizTxPersistenceException;
import com.mt.saga.domain.model.task.SubTaskStatus;
import com.mt.saga.domain.model.task.TaskStatus;
import com.mt.saga.domain.model.task.conclude_order_task.ConcludeOrderTask;
import com.mt.saga.domain.model.task.confirm_order_payment_task.ConfirmOrderPaymentTask;
import com.mt.saga.domain.model.task.create_order_task.CreateOrderTask;
import com.mt.saga.domain.model.task.recycle_order_task.RecycleOrderTask;
import com.mt.saga.domain.model.task.reserve_order_task.ReserveOrderTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
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

import static com.mt.saga.infrastructure.SpringStateMachineErrorHandler.ERROR_CLASS;

/**
 * each guard is an unit of work, roll back when failure happen
 */
@Configuration
@Slf4j
public class SpringStateMachineBuilder implements OrderStateMachineBuilder {

    public static final String TX_TASK = "TxTask";
    public static final String BIZ_ORDER = "BIZ_ORDER";

    @Autowired
    private OrderStateMachineApplicationService stateMachineApplicationService;

    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SpringStateMachineErrorHandler customStateMachineEventListener;

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
                    .withInternal()
                    .source(BizOrderStatus.NOT_PAID_RECYCLED)
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
            UserPlaceOrderEvent machineCommand = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
            log.info("start of recycle order with id {}", machineCommand.getOrderId());
            RecycleOrderTask task = context.getExtendedState().get(TX_TASK, RecycleOrderTask.class);
            // increase order storage
            CompletableFuture<Void> increaseOrderStorageFuture = CompletableFuture.runAsync(() -> {
                        List<PatchCommand> patchCommands = PatchCommand.buildRollbackCommand(DomainRegistry.getProductService().getReserveOrderPatchCommands(machineCommand.getProductList()));
                        DomainRegistry.getProductService().updateProductStorage(patchCommands, task.getTaskId());
                    }, customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    DomainRegistry.getOrderService().recycleOrder(machineCommand), customExecutor
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
            if (event.equals(BizOrderEvent.NEW_ORDER)) {
                log.info("start of save create order task");
                try {
                    new TransactionTemplate(transactionManager)
                            .execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // read task again make sure it's still valid & apply opt lock
                                    UserPlaceOrderEvent customerOrder = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
                                    CreateOrderTask bizTx = ApplicationServiceRegistry.getTaskApplicationService().createCreateOrderTask(customerOrder);
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
                                    UserPlaceOrderEvent customerOrder = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
                                    RecycleOrderTask task = ApplicationServiceRegistry.getTaskApplicationService().createRecycleOrderTask(customerOrder);
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
                                    UserPlaceOrderEvent customerOrder = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
                                    ReserveOrderTask task = ApplicationServiceRegistry.getTaskApplicationService().createReserveOrderTask(customerOrder);
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
                                    UserPlaceOrderEvent customerOrder = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
                                    ConfirmOrderPaymentTask task = ApplicationServiceRegistry.getTaskApplicationService().createConfirmOrderPaymentTask(customerOrder);
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
                                    UserPlaceOrderEvent customerOrder = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
                                    ConcludeOrderTask task = ApplicationServiceRegistry.getTaskApplicationService().createConcludeOrderTask(customerOrder);
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
                    UserPlaceOrderEvent bizOrder = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
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
            UserPlaceOrderEvent command = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
            CreateOrderTask bizTx = context.getExtendedState().get(TX_TASK, CreateOrderTask.class);
            Set<String> collect = command.getProductList().stream().map(CartDetail::getCartId).collect(Collectors.toSet());
            log.info("start of prepareNewOrder of {}", command.getOrderId());
            boolean b = DomainRegistry.getProductService().validateOrderedProduct(command.getProductList());
            if (!b)
                throw new BizOrderInvalidException();
            bizTx.getValidateOrderSubTask().setStatus(SubTaskStatus.COMPLETED);
            DomainEventPublisher.instance().publish(new GeneratePaymentQRLinkEvent(command.getOrderId(), bizTx.getChangeId(),bizTx.getId().toString()));
            DomainEventPublisher.instance().publish(
                    new DecreaseOrderStorageEvent(
                            DomainRegistry.getProductService().getReserveOrderPatchCommands(command.getProductList()),
                            bizTx.getChangeId(),
                            bizTx.getId()
                    ));
            DomainEventPublisher.instance().publish(new ClearCartEvent(command.getUserId(), collect, bizTx.getChangeId(), bizTx.getId()));
            DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(bizTx);
            return true;
        };
    }


    private Guard<BizOrderStatus, BizOrderEvent> reserveOrderTx() {
        return context -> {
            log.info("start of reserveOrderTx");
            UserPlaceOrderEvent stateMachineCommand = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
            ReserveOrderTask task = context.getExtendedState().get(TX_TASK, ReserveOrderTask.class);

            // decrease order storage
            CompletableFuture<Void> decreaseOrderStorageFuture = CompletableFuture.runAsync(() ->
                    DomainRegistry.getProductService().updateProductStorage(DomainRegistry.getProductService().getReserveOrderPatchCommands(stateMachineCommand.getProductList()), task.getTaskId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    DomainRegistry.getOrderService().reservedOrder(stateMachineCommand), customExecutor
            );

            List<RuntimeException> exs = new ArrayList<>();
            try {
                decreaseOrderStorageFuture.get();
                task.setDecreaseOrderStorageSubTaskStatus(SubTaskStatus.COMPLETED);
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
            DomainRegistry.getMessengerService().notifyBusinessOwner(new HashMap<>());
        };
    }

    private Guard<BizOrderStatus, BizOrderEvent> confirmOrderTx() {
        return context -> {
            UserPlaceOrderEvent machineCommand = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
            ConcludeOrderTask task = context.getExtendedState().get(TX_TASK, ConcludeOrderTask.class);
            log.info("start of decreaseActualStorage for {}", machineCommand.getOrderId());
            // decrease actual storage
            CompletableFuture<Void> decreaseActualStorageFuture = CompletableFuture.runAsync(() ->
                    DomainRegistry.getProductService().updateProductStorage(DomainRegistry.getProductService().getConfirmOrderPatchCommands(machineCommand.getProductList()), task.getTaskId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    DomainRegistry.getOrderService().concludeOrder(machineCommand), customExecutor
            );

            List<RuntimeException> exs = new ArrayList<>();
            try {
                decreaseActualStorageFuture.get();
                task.setDecreaseActualStorageSubTaskStatus(SubTaskStatus.COMPLETED);
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
                markConcludeOrderTaskAs(context, TaskStatus.FAILED);
                return false;
            }
            markConcludeOrderTaskAs(context, TaskStatus.COMPLETED);
            return true;
        };
    }

    private Guard<BizOrderStatus, BizOrderEvent> confirmPaymentTx() {
        return context -> {
            log.info("start of updatePaymentStatus");
            UserPlaceOrderEvent bizOrder = context.getExtendedState().get(BIZ_ORDER, UserPlaceOrderEvent.class);
            ConfirmOrderPaymentTask task = context.getExtendedState().get(TX_TASK, ConfirmOrderPaymentTask.class);

            // confirm payment
            CompletableFuture<Boolean> confirmPaymentFuture = CompletableFuture.supplyAsync(() ->
                    DomainRegistry.getPaymentService().confirmPaymentStatus(bizOrder.getOrderId()), customExecutor
            );

            // update order
            CompletableFuture<Void> updateOrderFuture = CompletableFuture.runAsync(() ->
                    DomainRegistry.getOrderService().confirmPayment(bizOrder), customExecutor
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
        o.setTaskStatus(txStatus);
        DomainRegistry.getCreateOrderTaskRepository().createOrUpdate(o);
    }

    private void markRecycleTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TaskStatus txStatus) {
        log.info("mark task as {}", txStatus);
        RecycleOrderTask o = (RecycleOrderTask) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTaskStatus(txStatus);
        DomainRegistry.getRecycleOrderTaskRepository().add(o);
    }

    private void markReserveTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TaskStatus txStatus) {
        log.info("mark task as {}", txStatus);
        ReserveOrderTask o = (ReserveOrderTask) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTaskStatus(txStatus);
        DomainRegistry.getReserveOrderTaskRepository().add(o);
    }

    private void markConfirmPaymentTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TaskStatus txStatus) {
        log.info("mark task as {}", txStatus);
        ConfirmOrderPaymentTask o = (ConfirmOrderPaymentTask) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTaskStatus(txStatus);
        DomainRegistry.getConfirmOrderPaymentTaskRepository().add(o);
    }

    private void markConcludeOrderTaskAs(StateContext<BizOrderStatus, BizOrderEvent> context, TaskStatus txStatus) {
        log.info("mark task as {}", txStatus);
        ConcludeOrderTask o = (ConcludeOrderTask) context.getExtendedState().getVariables().get(TX_TASK);
        o.setTaskStatus(txStatus);
        DomainRegistry.getConcludeOrderTaskRepository().add(o);
    }

    @Override
    public void handleEvent(UserPlaceOrderEvent event) {
        StateMachine<BizOrderStatus, BizOrderEvent> stateMachine = buildMachine(event.getOrderState());
        stateMachine.getExtendedState().getVariables().put(BIZ_ORDER, event);
        if (event.getPrepareEvent() != null) {
            stateMachine.sendEvent(event.getPrepareEvent());
            if (stateMachine.hasStateMachineError()) {
                throw stateMachine.getExtendedState().get(ERROR_CLASS, RuntimeException.class);
            }
        }
        stateMachine.sendEvent(event.getBizOrderEvent());
        if (stateMachine.hasStateMachineError()) {
            throw stateMachine.getExtendedState().get(ERROR_CLASS, RuntimeException.class);
        }
    }
}

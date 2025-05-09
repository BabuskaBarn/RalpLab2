package at.fhv.sysarch.lab2.ordersystem.internal;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
public class OrderProcessor {
    public interface Command {}

    public static final class ProcessOrder implements Command {
        public final String productName;
        public final int quantity;
        public final ActorRef<OrderResponse> replyTo;

        public ProcessOrder(String productName, int quantity, ActorRef<OrderResponse> replyTo) {
            this.productName = productName;
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }

    public static final class StartService implements Command {}
    public static final class StopService implements Command {}
    public static final class GetStatus implements Command {
        public final ActorRef<StatusResponse> replyTo;
        public GetStatus(ActorRef<StatusResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public interface OrderResponse {}
    public interface StatusResponse {}

    public static final class OrderSuccess implements OrderResponse {
        public final String orderId;
        public final String details;
        public OrderSuccess(String orderId, String details) {
            this.orderId = orderId;
            this.details = details;
        }
    }

    public static final class OrderFailed implements OrderResponse {
        public final String reason;
        public OrderFailed(String reason) {
            this.reason = reason;
        }
    }

    public static final class StatusReport implements StatusResponse {
        public final boolean isActive;
        public final int pendingOrders;
        public StatusReport(boolean isActive, int pendingOrders) {
            this.isActive = isActive;
            this.pendingOrders = pendingOrders;
        }
    }
}

/*
public class OrderProcessor extends AbstractBehavior<OrderProcessor.Command> {

    public interface Command {}

    // External order processing command
    public static final class ProcessOrder implements Command {
        public final String productName;
        public final int quantity;
        public final ActorRef<OrderResponse> replyTo;

        public ProcessOrder(String productName, int quantity, ActorRef<OrderResponse> replyTo) {
            this.productName = productName;
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }

    // Internal management commands
    public static final class StartService implements Command {}
    public static final class StopService implements Command {}
    public static final class GetStatus implements Command {
        public final ActorRef<StatusResponse> replyTo;

        public GetStatus(ActorRef<StatusResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    // Response messages
    public interface OrderResponse {}
    public interface StatusResponse {}

    public static final class OrderSuccess implements OrderResponse {
        public final String orderId;
        public final String details;

        public OrderSuccess(String orderId, String details) {
            this.orderId = orderId;
            this.details = details;
        }
    }

    public static final class OrderFailed implements OrderResponse {
        public final String reason;

        public OrderFailed(String reason) {
            this.reason = reason;
        }
    }

    public static final class StatusReport implements StatusResponse {
        public final boolean isActive;
        public final int pendingOrders;

        public StatusReport(boolean isActive, int pendingOrders) {
            this.isActive = isActive;
            this.pendingOrders = pendingOrders;
        }
    }

    private boolean isActive = true;
    private int pendingOrders = 0;

    public static Behavior<Command> create() {
        return Behaviors.setup(OrderProcessor::new);
    }

    private OrderProcessor(ActorContext<Command> context) {
        super(context);
        context.getLog().info("OrderProcessor started");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessOrder.class, this::onProcessOrder)
                .onMessage(StartService.class, this::onStartService)
                .onMessage(StopService.class, this::onStopService)
                .onMessage(GetStatus.class, this::onGetStatus)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onProcessOrder(ProcessOrder cmd) {
        if (!isActive) {
            cmd.replyTo.tell(new OrderFailed("Service is currently unavailable"));
            return this;
        }

        pendingOrders++;
        getContext().getLog().info("Processing order for {} x {} (Pending: {})",
                cmd.productName, cmd.quantity, pendingOrders);

        // Simuliere asynchrone Verarbeitung
        getContext().getScheduler().scheduleOnce(
                java.time.Duration.ofSeconds(1),
                getContext().getSelf(),
                new CompleteOrder(cmd)
        );

        return this;
    }

    private Behavior<Command> onStartService(StartService cmd) {
        isActive = true;
        getContext().getLog().info("OrderProcessor service activated");
        return this;
    }

    private Behavior<Command> onStopService(StopService cmd) {
        isActive = false;
        getContext().getLog().info("OrderProcessor service deactivated");
        return this;
    }

    private Behavior<Command> onGetStatus(GetStatus cmd) {
        cmd.replyTo.tell(new StatusReport(isActive, pendingOrders));
        return this;
    }

    private Behavior<Command> onPostStop() {
        getContext().getLog().info("OrderProcessor stopped with {} pending orders", pendingOrders);
        return this;
    }

    // Internal command for simulated order completion
    private static final class CompleteOrder implements Command {
        public final ProcessOrder originalOrder;

        public CompleteOrder(ProcessOrder originalOrder) {
            this.originalOrder = originalOrder;
        }
    }

    private Behavior<Command> onCompleteOrder(CompleteOrder cmd) {
        pendingOrders--;
        cmd.originalOrder.replyTo.tell(new OrderSuccess(
                "ORD-" + System.currentTimeMillis(),
                String.format("Processed %s x %d", cmd.originalOrder.productName, cmd.originalOrder.quantity)
        ));
        return this;
    }
}

 */
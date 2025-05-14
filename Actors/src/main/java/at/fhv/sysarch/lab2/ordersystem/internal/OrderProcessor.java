package at.fhv.sysarch.lab2.ordersystem.internal;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.UUID;

public class OrderProcessor extends AbstractBehavior<OrderProcessor.Command> {

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

    private static final class CompleteOrder implements Command {
        public final ProcessOrder originalOrder;

        public CompleteOrder(ProcessOrder originalOrder) {
            this.originalOrder = originalOrder;
        }
    }

    private boolean isActive = true;
    private int pendingOrders = 0;

    public static Behavior<Command> create() {
        return Behaviors.setup(OrderProcessor::new);
    }

    private OrderProcessor(ActorContext<Command> context) {
        super(context);
        context.getLog().info("OrderProcessor gestartet");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessOrder.class, this::onProcessOrder)
                .onMessage(StartService.class, this::onStartService)
                .onMessage(StopService.class, this::onStopService)
                .onMessage(GetStatus.class, this::onGetStatus)
                .onMessage(CompleteOrder.class, this::onCompleteOrder)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onProcessOrder(ProcessOrder cmd) {
        if (!isActive) {
            cmd.replyTo.tell(new OrderFailed("Service ist derzeit nicht verfügbar"));
            return this;
        }

        pendingOrders++;
        getContext().getLog().info("Empfange Bestellung: {} x {} (Pending Orders: {})",
                cmd.productName, cmd.quantity, pendingOrders);

        // Simuliere asynchrone Verarbeitung mit Delay
        getContext().getSystem().scheduler().scheduleOnce(
                java.time.Duration.ofSeconds(1),
                () -> getContext().getSelf().tell(new CompleteOrder(cmd)),
                getContext().getSystem().executionContext()
        );

        return this;
    }

    private Behavior<Command> onStartService(StartService cmd) {
        isActive = true;
        getContext().getLog().info("OrderProcessor aktiviert");
        return this;
    }

    private Behavior<Command> onStopService(StopService cmd) {
        isActive = false;
        getContext().getLog().info("OrderProcessor deaktiviert");
        return this;
    }

    private Behavior<Command> onGetStatus(GetStatus cmd) {
        cmd.replyTo.tell(new StatusReport(isActive, pendingOrders));
        return this;
    }

    private Behavior<Command> onCompleteOrder(CompleteOrder cmd) {
        pendingOrders--;
        String orderId = "ORD-" + UUID.randomUUID();
        String details = String.format("Bestellung erfolgreich: %s x %d",
                cmd.originalOrder.productName, cmd.originalOrder.quantity);

        getContext().getLog().info("Bestellung abgeschlossen: {} (Offen: {})", orderId, pendingOrders);
        cmd.originalOrder.replyTo.tell(new OrderSuccess(orderId, details));
        return this;
    }

    private Behavior<Command> onPostStop() {
        getContext().getLog().info("OrderProcessor wird gestoppt. Offene Bestellungen: {}", pendingOrders);
        return this;
    }
}

package at.fhv.sysarch.lab2.homeautomation.devices;




import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class OrderProcessor extends AbstractBehavior<OrderProcessor.OrderCommand> {

    public interface OrderCommand {}

    public static final class ProcessOrder implements OrderCommand {
        final String productName;
        final int quantity;
        final ActorRef<Fridge.OrderResponse> replyTo;

        public ProcessOrder(String productName, int quantity, ActorRef<Fridge.OrderResponse> replyTo) {
            this.productName = productName;
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }

    public static Behavior<OrderCommand> create() {
        return Behaviors.setup(OrderProcessor::new);
    }

    private OrderProcessor(ActorContext<OrderCommand> context) {
        super(context);
        context.getLog().info("OrderProcessor started");
    }

    @Override
    public Receive<OrderCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessOrder.class, this::onProcessOrder)
                .build();
    }

    private Behavior<OrderCommand> onProcessOrder(ProcessOrder order) {
        getContext().getLog().info("Processing order for {} x {}", order.productName, order.quantity);

        // Hier würde normalerweise die gRPC-Kommunikation stattfinden
        // Für die Simulation geben wir einfach eine erfolgreiche Antwort zurück
        order.replyTo.tell(new Fridge.OrderAccepted(
                String.format("Order processed: %s x %d", order.productName, order.quantity)
        ));

        return this;
    }
}

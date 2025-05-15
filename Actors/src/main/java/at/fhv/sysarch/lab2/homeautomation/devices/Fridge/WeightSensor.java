package at.fhv.sysarch.lab2.homeautomation.devices.Fridge;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class WeightSensor extends AbstractBehavior<WeightSensor.WeightSensorCommand>{

    public interface WeightSensorCommand {
    }

    public static final class UpdateWeight implements WeightSensorCommand {
        final double weight;
        public UpdateWeight(double weight){
            this.weight = weight;
        }
    }

    public static final class QueryWeight implements WeightSensorCommand {
        public final ActorRef<OrderProcessor.OrderProcessorCommand> replyTo;

        public QueryWeight(ActorRef<OrderProcessor.OrderProcessorCommand> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class RespondWeight {
        public final double weight;

        public RespondWeight(double weight) {
            this.weight = weight;
        }
    }

    private double _weight;
    private double _maxWeight;

    private WeightSensor(ActorContext<WeightSensorCommand> context, double maxWeight){
        super(context);
        _maxWeight = maxWeight;
    }

    public static Behavior<WeightSensorCommand> create(double maxWeight){
        return Behaviors.setup(context -> new WeightSensor(context, maxWeight));
    }

    @Override
    public Receive<WeightSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateWeight.class, this::onUpdateWeight)
                .onMessage(QueryWeight.class, this::onQueryAvailableWeight)
                .build();
    }

    private Behavior<WeightSensorCommand> onUpdateWeight(UpdateWeight command) {
        _weight = command.weight;
        getContext().getLog().info("Fridge taken weight: {}", _weight);
        return this;
    }

    private Behavior<WeightSensorCommand> onQueryAvailableWeight(QueryWeight command) {
        command.replyTo.tell(new OrderProcessor.ReceiveWeight(_maxWeight - _weight));
        return this;
    }
}

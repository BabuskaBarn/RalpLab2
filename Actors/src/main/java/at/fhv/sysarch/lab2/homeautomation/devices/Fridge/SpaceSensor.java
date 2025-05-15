package at.fhv.sysarch.lab2.homeautomation.devices.Fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class SpaceSensor extends AbstractBehavior<SpaceSensor.SpaceSensorCommand>{

    public interface SpaceSensorCommand {
    }

    public static final class UpdateSpace implements SpaceSensorCommand {
        final int space;
        public UpdateSpace(int space){
            this.space = space;
        }
    }

    public static final class QuerySpace implements SpaceSensorCommand {
        public final ActorRef<OrderProcessor.OrderProcessorCommand> replyTo;

        public QuerySpace(ActorRef<OrderProcessor.OrderProcessorCommand> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class RespondSpace {
        public final int space;

        public RespondSpace(int space) {
            this.space = space;
        }
    }

    private int _space;
    private int _maxSpace;

    private SpaceSensor(ActorContext<SpaceSensorCommand> context, int maxSpace){
        super(context);
        _maxSpace = maxSpace;
    }

    public static Behavior<SpaceSensorCommand> create(int maxSpace){
        return Behaviors.setup(context -> new SpaceSensor(context, maxSpace));
    }
    @Override
    public Receive<SpaceSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateSpace.class, this::onUpdateSpace)
                .onMessage(QuerySpace.class, this::onQueryAvailableSpace)
                .build();
    }

    private Behavior<SpaceSensorCommand> onUpdateSpace(UpdateSpace command) {
        _space = command.space;
        getContext().getLog().info("Fridge taken space: {}", _space);
        return this;
    }

    private Behavior<SpaceSensorCommand> onQueryAvailableSpace(QuerySpace command) {
        command.replyTo.tell(new OrderProcessor.ReceiveSpace(_maxSpace - _space));
        return this;
    }
}

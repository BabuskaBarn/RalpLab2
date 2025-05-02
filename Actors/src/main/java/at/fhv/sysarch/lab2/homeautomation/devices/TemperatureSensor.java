package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.TemperatureCommand> {

    public interface TemperatureCommand {}

    public static final class ReadTemperature implements TemperatureCommand {
        final Double value;

        public ReadTemperature(Double value) {
            this.value = value;
        }
    }

    public static Behavior<TemperatureCommand> create(ActorRef<AirCondition.AirConditionCommand> airCondition) {
        return Behaviors.setup(context -> new TemperatureSensor(context, airCondition));
    }

    private final ActorRef<AirCondition.AirConditionCommand> airCondition;

    public TemperatureSensor(ActorContext<TemperatureCommand> context, ActorRef<AirCondition.AirConditionCommand> airCondition) {
        super(context);
        this.airCondition = airCondition;

        getContext().getLog().info("TemperatureSensor started");
    }

    @Override
    public Receive<TemperatureCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadTemperature.class, this::onReadTemperature)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<TemperatureCommand> onReadTemperature(ReadTemperature r) {
        getContext().getLog().info("TemperatureSensor received {}", r.value);
        this.airCondition.tell(new AirCondition.EnrichedTemperature(r.value, "Celsius"));
        return this;
    }

    private TemperatureSensor onPostStop() {
        getContext().getLog().info("TemperatureSensor actor {}-{} stopped");
        return this;
    }

}

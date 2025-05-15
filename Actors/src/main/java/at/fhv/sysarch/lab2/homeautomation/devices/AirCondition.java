package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class AirCondition extends AbstractBehavior<AirCondition.AirConditionCommand> {

    public interface AirConditionCommand {}

    public static final class PowerAirCondition implements AirConditionCommand {
        public final Boolean value;

        public PowerAirCondition(Boolean value) {
            this.value = value;
        }
    }

    public static final class EnrichedTemperature implements AirConditionCommand {
        public final Double value;
        public final String unit;

        public EnrichedTemperature(Double value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    private final String identifier;
    private boolean isPoweredOn = false;
    private static final double THRESHOLD = 22.0;

    public AirCondition(ActorContext<AirConditionCommand> context, String identifier) {
        super(context);
        this.identifier = identifier;
        getContext().getLog().info("AirCondition [{}] started", identifier);
    }

    public static Behavior<AirConditionCommand> create(String identifier) {
        return Behaviors.setup(context -> new AirCondition(context, identifier));
    }

    @Override
    public Receive<AirConditionCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(EnrichedTemperature.class, this::onReadTemperature)
                .onMessage(PowerAirCondition.class, this::onManualPowerToggle)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<AirConditionCommand> onReadTemperature(EnrichedTemperature msg) {
        getContext().getLog().info("AirCondition [{}] received temperature: {} {}", identifier, msg.value, msg.unit);

        if (msg.value > THRESHOLD && !isPoweredOn) {
            isPoweredOn = true;
            getContext().getLog().info("AirCondition [{}] switched ON (cooling)", identifier);
        } else if (msg.value <= THRESHOLD && isPoweredOn) {
            isPoweredOn = false;
            getContext().getLog().info("AirCondition [{}] switched OFF", identifier);
        } else {
            getContext().getLog().info("AirCondition [{}] remains {}", identifier, isPoweredOn ? "ON" : "OFF");
        }

        return this;
    }

    private Behavior<AirConditionCommand> onManualPowerToggle(PowerAirCondition cmd) {
        isPoweredOn = cmd.value;
        getContext().getLog().info("AirCondition [{}] manually set to: {}", identifier, isPoweredOn ? "ON" : "OFF");
        return this;
    }

    private AirCondition onPostStop() {
        getContext().getLog().info("AirCondition [{}] stopped", identifier);
        return this;
    }
}

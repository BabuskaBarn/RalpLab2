package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.simulators.TemperatureSimulator;
import at.fhv.sysarch.lab2.homeautomation.mqtt.MqttSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.TemperatureCommand> {

    public interface TemperatureCommand {}

    public static final class ToggleSource implements TemperatureCommand {
        public final boolean useExternal;
        public ToggleSource(boolean useExternal) {
            this.useExternal = useExternal;
        }
    }

    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ObjectMapper mapper = new ObjectMapper();
    private double currentTemperature = 20.0;
    private boolean useExternalSource = false;

    public static Behavior<TemperatureCommand> create(ActorRef<AirCondition.AirConditionCommand> airCondition) {
        return Behaviors.setup(context -> new TemperatureSensor(context, airCondition));
    }

    private TemperatureSensor(ActorContext<TemperatureCommand> context, ActorRef<AirCondition.AirConditionCommand> airCondition) {
        super(context);
        this.airCondition = airCondition;
        getContext().getLog().info("TemperatureSensor started");
    }

    @Override
    public Receive<TemperatureCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureSimulator.ForwardTemperatureCommand.class, this::onSimulatorTemperature)
                .onMessage(MqttSubscriber.ForwardTemperatureFromMqtt.class, this::onMqttTemperature)
                .onMessage(ToggleSource.class, this::onToggleSource)
                .build();
    }

    private Behavior<TemperatureCommand> onToggleSource(ToggleSource cmd) {
        useExternalSource = cmd.useExternal;
        getContext().getLog().info("TemperatureSensor toggled to {}", useExternalSource ? "MQTT (external)" : "Simulation (internal)");
        return this;
    }

    private Behavior<TemperatureCommand> onSimulatorTemperature(TemperatureSimulator.ForwardTemperatureCommand cmd) {
        if (!useExternalSource) {
            currentTemperature = cmd.getTemperature();
            getContext().getLog().info("TemperatureSensor received simulation temperature: {} °C", currentTemperature);
            notifyAirCondition();
        }
        return this;
    }

    private Behavior<TemperatureCommand> onMqttTemperature(MqttSubscriber.ForwardTemperatureFromMqtt cmd) {
        if (useExternalSource) {
            currentTemperature = cmd.getTemperature();
            getContext().getLog().info("TemperatureSensor received MQTT temperature: {} °C", currentTemperature);
            notifyAirCondition();
        }
        return this;
    }

    private void notifyAirCondition() {
        this.airCondition.tell(new AirCondition.EnrichedTemperature(currentTemperature, "Celsius"));
    }
}

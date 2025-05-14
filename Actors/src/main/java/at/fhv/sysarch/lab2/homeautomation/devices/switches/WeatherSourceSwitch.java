package at.fhv.sysarch.lab2.homeautomation.devices.switches;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.mqtt.MqttSubscriber;

public class WeatherSourceSwitch extends AbstractBehavior<WeatherSourceSwitch.Command> {

    public interface Command {}

    public static class SetSimulationSource implements Command {
        public final boolean useExternalSource;

        public SetSimulationSource(boolean useExternalSource) {
            this.useExternalSource = useExternalSource;
        }
    }

    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private final ActorRef<Environment.EnvironmentCommand> weatherSimulator;
    private final ActorRef<MqttSubscriber.Command> mqttSubscriber;

    public static Behavior<Command> create(
            ActorRef<WeatherSensor.WeatherCommand> weatherSensor,
            ActorRef<Environment.EnvironmentCommand> weatherSimulator,
            ActorRef<MqttSubscriber.Command> mqttSubscriber) {
        return Behaviors.setup(ctx -> new WeatherSourceSwitch(ctx, weatherSensor, weatherSimulator, mqttSubscriber));
    }

    private WeatherSourceSwitch(
            ActorContext<Command> context,
            ActorRef<WeatherSensor.WeatherCommand> weatherSensor,
            ActorRef<Environment.EnvironmentCommand> weatherSimulator,
            ActorRef<MqttSubscriber.Command> mqttSubscriber) {
        super(context);
        this.weatherSensor = weatherSensor;
        this.weatherSimulator = weatherSimulator;
        this.mqttSubscriber = mqttSubscriber;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SetSimulationSource.class, this::onSetSimulationSource)
                .build();
    }

    private Behavior<Command> onSetSimulationSource(SetSimulationSource cmd) {
        if (cmd.useExternalSource) {
            getContext().getLog().info("Switching to external source (MQTT)");
            weatherSensor.tell(new WeatherSensor.ReadWeather(true));  // Using MQTT as external source
            weatherSimulator.tell(new Environment.DeactivateSimulation());
        } else {
            getContext().getLog().info("Switching to internal WeatherSimulator");
            weatherSensor.tell(new WeatherSensor.ReadWeather(false));  // Using internal simulation
            weatherSimulator.tell(new Environment.ActivateSimulation());
        }

        return this;
    }
}

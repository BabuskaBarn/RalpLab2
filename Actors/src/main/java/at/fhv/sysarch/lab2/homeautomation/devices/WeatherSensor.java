package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.simulators.WeatherSimulator;
import at.fhv.sysarch.lab2.homeautomation.mqtt.MqttSubscriber;
import at.fhv.sysarch.lab2.homeautomation.devices.states.WeatherState;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand {}

    public static final class ReadWeather implements WeatherCommand {
        final boolean isSunny;
        public ReadWeather(boolean isSunny) {
            this.isSunny = isSunny;
        }
    }

    public static final class ToggleSource implements WeatherCommand {
        public final boolean useExternal;
        public ToggleSource(boolean useExternal) {
            this.useExternal = useExternal;
        }
    }

    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final ObjectMapper mapper = new ObjectMapper();
    private boolean useExternalSource = false;
    private WeatherState currentWeatherState = WeatherState.SUNNY;

    public static Behavior<WeatherCommand> create(ActorRef<Blinds.BlindsCommand> blinds) {
        return Behaviors.setup(context -> new WeatherSensor(context, blinds));
    }

    private WeatherSensor(ActorContext<WeatherCommand> context, ActorRef<Blinds.BlindsCommand> blinds) {
        super(context);
        this.blinds = blinds;
        getContext().getLog().info("WeatherSensor started");
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadWeather.class, this::onReadWeather)
                .onMessage(WeatherSimulator.ForwardWeatherCommand.class, this::onSimulatorWeather)
                .onMessage(MqttSubscriber.ForwardWeatherFromMqtt.class, this::onMqttWeather)
                .onMessage(ToggleSource.class, this::onToggleSource)
                .build();
    }

    private Behavior<WeatherCommand> onToggleSource(ToggleSource cmd) {
        useExternalSource = cmd.useExternal;
        getContext().getLog().info("WeatherSensor toggled to {}", useExternalSource ? "MQTT (external)" : "Simulation (internal)");
        return this;
    }

    private Behavior<WeatherCommand> onReadWeather(ReadWeather r) {
        if (!useExternalSource) {
            currentWeatherState = r.isSunny ? WeatherState.SUNNY : WeatherState.CLOUDY;
            getContext().getLog().info("WeatherSensor received internal simulation data: {}", currentWeatherState);
            notifyBlinds();
        }
        return this;
    }

    private Behavior<WeatherCommand> onSimulatorWeather(WeatherSimulator.ForwardWeatherCommand command) {
        if (!useExternalSource && command != null) {
            currentWeatherState = command.getWeatherState();
            getContext().getLog().info("WeatherSensor received simulation weather: {}", currentWeatherState);
            notifyBlinds();
        }
        return this;
    }

    private Behavior<WeatherCommand> onMqttWeather(MqttSubscriber.ForwardWeatherFromMqtt command) {
        if (useExternalSource && command != null) {
            currentWeatherState = command.getWeatherState();
            getContext().getLog().info("WeatherSensor received external MQTT weather: {}", currentWeatherState);
            notifyBlinds();
        }
        return this;
    }

    private void notifyBlinds() {
        boolean isSunny = currentWeatherState == WeatherState.SUNNY;
        this.blinds.tell(new Blinds.WeatherChanged(isSunny));
    }
}

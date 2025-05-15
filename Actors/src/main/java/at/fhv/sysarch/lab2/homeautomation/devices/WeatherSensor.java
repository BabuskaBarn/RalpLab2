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

    // Für interne Simulation
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

    // Für externe MQTT-Daten (mit String statt Enum)
    public static final class ExternalWeatherUpdate implements WeatherCommand {
        public final String condition;
        public ExternalWeatherUpdate(String condition) {
            this.condition = condition;
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
                .onMessage(ExternalWeatherUpdate.class, this::onExternalWeather)
                .onMessage(ToggleSource.class, this::onToggleSource)
                .build();
    }

    private Behavior<WeatherCommand> onToggleSource(ToggleSource cmd) {
        useExternalSource = cmd.useExternal;
        getContext().getLog().info("WeatherSensor toggled to {}",
                useExternalSource ? "external source" : "internal simulation");
        return this;
    }

    private Behavior<WeatherCommand> onReadWeather(ReadWeather r) {
        if (!useExternalSource) {
            currentWeatherState = r.isSunny ? WeatherState.SUNNY : WeatherState.CLOUDY;
            getContext().getLog().info("Internal weather update: {}", currentWeatherState);
            notifyBlinds();
        }
        return this;
    }

    private Behavior<WeatherCommand> onSimulatorWeather(WeatherSimulator.ForwardWeatherCommand command) {
        if (!useExternalSource && command != null) {
            currentWeatherState = command.getWeatherState();
            getContext().getLog().info("Simulator weather update: {}", currentWeatherState);
            notifyBlinds();
        }
        return this;
    }

    private Behavior<WeatherCommand> onExternalWeather(ExternalWeatherUpdate command) {
        if (useExternalSource && command != null) {
            // Konvertierung des String-Werts in WeatherState mit Fallback
            try {
                currentWeatherState = WeatherState.valueOf(command.condition.toUpperCase());
            } catch (IllegalArgumentException e) {
                currentWeatherState = mapToWeatherState(command.condition);
                getContext().getLog().warn("Unknown weather condition '{}', mapped to {}",
                        command.condition, currentWeatherState);
            }
            getContext().getLog().info("External weather update: {}", currentWeatherState);
            notifyBlinds();
        }
        return this;
    }

    private WeatherState mapToWeatherState(String condition) {
        return switch (condition.toLowerCase()) {
            case "rain", "snow", "storm" -> WeatherState.STORMY;
            case "fog", "mist" -> WeatherState.FOGGY;
            case "cloudy", "overcast" -> WeatherState.CLOUDY;
            default -> WeatherState.SUNNY; // Default bei unbekannten Werten
        };
    }

    private void notifyBlinds() {
        boolean isSunny = currentWeatherState == WeatherState.SUNNY;
        this.blinds.tell(new Blinds.WeatherChanged(isSunny));
    }
}
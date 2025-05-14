package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
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

    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final ObjectMapper mapper = new ObjectMapper();
    private boolean isExternalSource = false;
    private WeatherState currentWeatherState = WeatherState.SUNNY;  // Default to sunny

    public static Behavior<WeatherCommand> create(ActorRef<Blinds.BlindsCommand> blinds) {
        return Behaviors.setup(context -> new WeatherSensor(context, blinds));
    }

    private WeatherSensor(ActorContext<WeatherCommand> context, ActorRef<Blinds.BlindsCommand> blinds) {
        super(context);
        this.blinds = blinds;
        getContext().getLog().info("WeatherSensor started");
    }

    public void setExternalSource(boolean isExternalSource) {
        this.isExternalSource = isExternalSource;
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadWeather.class, this::onReadWeather)
                .onMessage(WeatherSimulator.ForwardWeatherCommand.class, this::onSimulatorWeather)
                .onMessage(MqttSubscriber.ForwardWeatherFromMqtt.class, this::onMqttWeather)
                .build();
    }

    // Verarbeitung der ReadWeather-Nachricht (vom Simulator)
    private Behavior<WeatherCommand> onReadWeather(ReadWeather r) {
        getContext().getLog().info("WeatherSensor received internal simulation data: {}", r.isSunny ? "sunny" : "not sunny");
        currentWeatherState = r.isSunny ? WeatherState.SUNNY : WeatherState.CLOUDY;
        notifyBlinds();
        return this;
    }

    // Verarbeitung von Wetterdaten vom Simulator
    private Behavior<WeatherCommand> onSimulatorWeather(WeatherSimulator.ForwardWeatherCommand command) {
        if (command != null) {
            currentWeatherState = command.getWeatherState();
            getContext().getLog().info("WeatherSensor received weather simulation data: {}", currentWeatherState);
            notifyBlinds();
        }
        return this;
    }

    // Verarbeitung von Wetterdaten aus MQTT
    private Behavior<WeatherCommand> onMqttWeather(MqttSubscriber.ForwardWeatherFromMqtt command) {
        if (command != null) {
            currentWeatherState = command.getWeatherState();
            getContext().getLog().info("WeatherSensor received external MQTT weather data: {}", currentWeatherState);
            notifyBlinds();
        }
        return this;
    }

    // Benachrichtigung der Blinds über den neuen Wetterzustand
    private void notifyBlinds() {
        boolean isSunny = currentWeatherState == WeatherState.SUNNY;
        this.blinds.tell(new Blinds.WeatherChanged(isSunny));
    }
}

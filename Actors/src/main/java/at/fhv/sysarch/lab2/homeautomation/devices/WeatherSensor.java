package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.mqtt.MqttService;
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
    private final MqttService mqttService;
    private final ObjectMapper mapper = new ObjectMapper();

    public static Behavior<WeatherCommand> create(
            ActorRef<Blinds.BlindsCommand> blinds,
            MqttService mqttService) {
        return Behaviors.setup(context -> new WeatherSensor(context, blinds, mqttService));
    }

    private WeatherSensor(
            ActorContext<WeatherCommand> context,
            ActorRef<Blinds.BlindsCommand> blinds,
            MqttService mqttService) {
        super(context);
        this.blinds = blinds;
        this.mqttService = mqttService;

        // Start weather simulation
        simulateWeatherChanges();

        getContext().getLog().info("WeatherSensor started");
    }

    private void simulateWeatherChanges() {
        getContext().getSystem().scheduler().scheduleAtFixedRate(
                java.time.Duration.ofSeconds(1),
                java.time.Duration.ofSeconds(10),
                () -> {
                    boolean isSunny = Math.random() > 0.5;
                    getContext().getSelf().tell(new ReadWeather(isSunny));
                },
                getContext().getSystem().executionContext()
        );
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadWeather.class, this::onReadWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeatherCommand> onReadWeather(ReadWeather r) {
        getContext().getLog().info("WeatherSensor received {}", r.isSunny ? "sunny" : "not sunny");

        // Send to Blinds via Akka
        this.blinds.tell(new Blinds.WeatherChanged(r.isSunny));

        // Publish via MQTT
        try {
            mqttService.publish("home/weather",
                    mapper.writeValueAsString(new WeatherReading(r.isSunny)));
        } catch (Exception e) {
            getContext().getLog().error("Failed to publish weather", e);
        }

        return this;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor stopped");
        return this;
    }

    private static class WeatherReading {
        public boolean isSunny;
        public WeatherReading(boolean sunny) { this.isSunny = sunny; }
    }
}
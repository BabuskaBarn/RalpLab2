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

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.TemperatureCommand> {
    public interface TemperatureCommand {}

    public static final class ReadTemperature implements TemperatureCommand {
        final Double value;
        public ReadTemperature(Double value) {
            this.value = value;
        }
    }

    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final MqttService mqttService;
    private final ObjectMapper mapper = new ObjectMapper();

    public static Behavior<TemperatureCommand> create(
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            MqttService mqttService) {
        return Behaviors.setup(context -> new TemperatureSensor(context, airCondition, mqttService));
    }

    private TemperatureSensor(
            ActorContext<TemperatureCommand> context,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            MqttService mqttService) {
        super(context);
        this.airCondition = airCondition;
        this.mqttService = mqttService;

        // Start temperature simulation
        simulateTemperatureChanges();

        getContext().getLog().info("TemperatureSensor started");
    }

    private void simulateTemperatureChanges() {
        getContext().getSystem().scheduler().scheduleAtFixedRate(
                java.time.Duration.ofSeconds(1),
                java.time.Duration.ofSeconds(5),
                () -> {
                    double temp = 15 + Math.random() * 15; // 15-30°C
                    getContext().getSelf().tell(new ReadTemperature(temp));
                },
                getContext().getSystem().executionContext()
        );
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

        // Send to AirCondition via Akka
        this.airCondition.tell(new AirCondition.EnrichedTemperature(r.value, "Celsius"));

        // Publish via MQTT
        try {
            mqttService.publish("home/temperature",
                    mapper.writeValueAsString(new TempReading(r.value)));
        } catch (Exception e) {
            getContext().getLog().error("Failed to publish temperature", e);
        }

        return this;
    }

    private TemperatureSensor onPostStop() {
        getContext().getLog().info("TemperatureSensor actor stopped");
        return this;
    }

    private static class TempReading {
        public double temperature;
        public TempReading(double temp) { this.temperature = temp; }
    }
}
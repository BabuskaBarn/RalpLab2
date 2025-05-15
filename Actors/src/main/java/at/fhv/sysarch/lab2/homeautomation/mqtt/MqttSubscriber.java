package at.fhv.sysarch.lab2.homeautomation.mqtt;

import akka.actor.typed.Behavior;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.TemperatureMessage;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.WeatherConditionMessage;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.states.WeatherState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;

public class MqttSubscriber {

    public interface Command {}

    public static class MqttEnvelope implements Command {
        public final String topic;
        public final String payload;

        public MqttEnvelope(String topic, String payload) {
            this.topic = topic;
            this.payload = payload;
        }
    }

    public static class ForwardWeatherFromMqtt implements WeatherSensor.WeatherCommand {
        private final WeatherState weatherState;

        public ForwardWeatherFromMqtt(WeatherState weatherState) {
            this.weatherState = weatherState;
        }

        public WeatherState getWeatherState() {
            return weatherState;
        }
    }

    public static class ForwardTemperatureFromMqtt implements TemperatureSensor.TemperatureCommand {
        private final double temperature;

        public ForwardTemperatureFromMqtt(double temperature) {
            this.temperature = temperature;
        }

        public double getTemperature() {
            return temperature;
        }
    }

    public static Behavior<Command> create(
            ActorRef<WeatherSensor.WeatherCommand> weatherSensor,
            ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor) {

        return Behaviors.setup(context -> {
            ObjectMapper mapper = new ObjectMapper();

            try {
                MqttClient mqttClient = new MqttClient("tcp://10.0.40.161:1883", MqttClient.generateClientId());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setConnectionTimeout(5); // 5 Sekunden Timeout

                mqttClient.connect(options);

                if (mqttClient.isConnected()) {
                    context.getLog().info("MQTT connection successful.");

                    mqttClient.subscribe("weather/condition", (topic, msg) -> {
                        String json = new String(msg.getPayload());
                        context.getSelf().tell(new MqttEnvelope(topic, json));
                    });

                    mqttClient.subscribe("weather/temperature", (topic, msg) -> {
                        String json = new String(msg.getPayload());
                        context.getSelf().tell(new MqttEnvelope(topic, json));
                    });

                    context.getLog().info("MQTT Subscriber subscribed to topics.");
                } else {
                    context.getLog().error("MQTT client not connected. Skipping subscriptions.");
                }
            } catch (Exception e) {
                context.getLog().error("Fehler beim MQTT-Setup: {}", e.getMessage());
            }

            return Behaviors.receive(Command.class)
                    .onMessage(MqttEnvelope.class, msg -> {
                        try {
                            if (msg.topic.equals("weather/condition")) {
                                WeatherConditionMessage condMsg = mapper.readValue(msg.payload, WeatherConditionMessage.class);
                                try {
                                    WeatherState state = WeatherState.valueOf(condMsg.condition.toUpperCase());
                                    weatherSensor.tell(new ForwardWeatherFromMqtt(state));
                                    context.getLog().info("Weather condition received: {}", state);
                                } catch (IllegalArgumentException e) {
                                    context.getLog().warn("Unbekannter WeatherState: {}", condMsg.condition);
                                }
                            } else if (msg.topic.equals("weather/temperature")) {
                                TemperatureMessage tempMsg = mapper.readValue(msg.payload, TemperatureMessage.class);
                                double temp = Double.parseDouble(tempMsg.temperature);
                                temperatureSensor.tell(new ForwardTemperatureFromMqtt(temp));
                                context.getLog().info("Temperature received: {}", temp);
                            } else {
                                context.getLog().warn("Unbekanntes Topic: {}", msg.topic);
                            }
                        } catch (Exception e) {
                            context.getLog().error("Fehler beim Verarbeiten der MQTT-Nachricht: {}", e.getMessage());
                        }

                        return Behaviors.same();
                    })
                    .build();
        });
    }
}

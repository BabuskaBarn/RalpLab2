package at.fhv.sysarch.lab2.homeautomation.mqtt;

import akka.actor.typed.Behavior;
import akka.actor.typed.ActorRef;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.TemperatureMessage;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.WeatherConditionMessage;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

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
            MqttClient mqttClient = null;

            try {
                mqttClient = new MqttClient("tcp://10.0.40.161:1883",
                        MqttClient.generateClientId(),
                        new MemoryPersistence());

                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setAutomaticReconnect(true);
                options.setConnectionTimeout(10);
                options.setKeepAliveInterval(60);

                // Callback für Verbindungsstatus
                mqttClient.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        context.getLog().warn("MQTT Verbindung verloren: {}", cause.getMessage());
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        context.getLog().debug("MQTT Nachricht empfangen - Topic: {}, QoS: {}",
                                topic, message.getQos());
                        context.getSelf().tell(new MqttEnvelope(topic, new String(message.getPayload())));
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        context.getLog().debug("Nachricht zugestellt: {}", token.getMessageId());
                    }
                });

                mqttClient.connect(options);

                // Subscribe mit QoS Level 1
                mqttClient.subscribe("weather/condition", 1);
                mqttClient.subscribe("weather/temperature", 1);

                context.getLog().info("MQTT Subscriber erfolgreich verbunden und subscribed");

            } catch (Exception e) {
                context.getLog().error("Kritischer MQTT-Fehler: {}", e.getMessage());
                if (mqttClient != null) {
                    try {
                        mqttClient.disconnect();
                        mqttClient.close();
                    } catch (MqttException ex) {
                        context.getLog().error("Fehler beim Bereinigen der MQTT-Verbindung: {}", ex.getMessage());
                    }
                }
                return Behaviors.stopped();
            }

            MqttClient finalMqttClient = mqttClient;
            return Behaviors.receive(Command.class)
                    .onMessage(MqttEnvelope.class, msg -> {
                        context.getLog().debug("Verarbeite MQTT-Nachricht - Topic: {}, Länge: {}",
                                msg.topic, msg.payload.length());

                        try {
                            if (msg.topic.equals("weather/condition")) {
                                WeatherConditionMessage condMsg = mapper.readValue(msg.payload, WeatherConditionMessage.class);
                                context.getLog().info("Empfangene Wetterbedingung: {}", condMsg.condition);
                                weatherSensor.tell(new WeatherSensor.ExternalWeatherUpdate(condMsg.condition));
                            }
                            else if (msg.topic.equals("weather/temperature")) {
                                TemperatureMessage tempMsg = mapper.readValue(msg.payload, TemperatureMessage.class);
                                try {
                                    double temp = Double.parseDouble(tempMsg.temperature);
                                    context.getLog().info("Empfangene Temperatur: {}", temp);
                                    temperatureSensor.tell(new ForwardTemperatureFromMqtt(temp));
                                } catch (NumberFormatException e) {
                                    context.getLog().error("Ungültiges Temperaturformat: {}", tempMsg.temperature);
                                }
                            }
                            else {
                                context.getLog().warn("Unbekanntes Topic: {}", msg.topic);
                            }
                        } catch (Exception e) {
                            context.getLog().error("Verarbeitungsfehler für Topic {}: {}", msg.topic, e.getMessage());
                        }
                        return Behaviors.same();
                    })
                    .onSignal(PostStop.class, signal -> {
                        if (finalMqttClient != null && finalMqttClient.isConnected()) {
                            try {
                                finalMqttClient.disconnect();
                                finalMqttClient.close();
                                context.getLog().info("MQTT Verbindung ordnungsgemäß getrennt");
                            } catch (MqttException e) {
                                context.getLog().error("Fehler beim Trennen der MQTT-Verbindung: {}", e.getMessage());
                            }
                        }
                        return Behaviors.same();
                    })
                    .build();
        });
    }
}
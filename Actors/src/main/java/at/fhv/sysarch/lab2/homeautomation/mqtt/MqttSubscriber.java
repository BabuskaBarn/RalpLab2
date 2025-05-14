package at.fhv.sysarch.lab2.homeautomation.mqtt;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment.SetWeather;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.WeatherConditionMessage;
import at.fhv.sysarch.lab2.homeautomation.devices.simulators.WeatherSimulator;
import at.fhv.sysarch.lab2.homeautomation.devices.states.WeatherState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttSubscriber extends AbstractActor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ActorRef weatherSimulator;

    public MqttSubscriber(ActorRef weatherSimulator) {
        this.weatherSimulator = weatherSimulator;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(MqttMessage.class, this::handleMqttMessage)
                .build();
    }

    private void handleMqttMessage(MqttMessage message) {
        try {
            String json = new String(message.getPayload());
            WeatherConditionMessage conditionMessage = objectMapper.readValue(json, WeatherConditionMessage.class);

            // Beispiel: Bedingung "snow", "sunny", "rain"
            String condition = conditionMessage.condition.toUpperCase();
            WeatherState state = WeatherState.valueOf(condition);

            weatherSimulator.tell(new SetWeather(state), getSelf());

        } catch (Exception e) {
            System.err.println("Fehler beim Verarbeiten der MQTT-Nachricht: " + e.getMessage());
        }
    }
}

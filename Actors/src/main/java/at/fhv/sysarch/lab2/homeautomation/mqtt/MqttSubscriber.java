package at.fhv.sysarch.lab2.homeautomation.mqtt;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.typed.javadsl.AbstractBehavior;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.Enviornment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class MqttSubscriber extends AbstractBehavior<Enviornment.EnvironmentCommand> {

    private final ActorRef temperatureActor;
    private final ActorRef weatherActor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MqttSubscriberActor(ActorRef temperatureActor, ActorRef weatherActor) {
        this.temperatureActor = temperatureActor;
        this.weatherActor = weatherActor;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(MqttMessage.class, this::handleMqttMessage)
                .build();
    }

    private void handleMqttMessage(MqttMessage message) {
        try {
            EnvironmentCommand command = objectMapper.readValue(
                    message.getPayload(),
                    EnvironmentCommand.class
            );

            // Route command to appropriate actor
            switch (command.getType()) {
                case TEMPERATURE_CHANGE:
                    temperatureActor.tell(command, getSelf());
                    break;
                case WEATHER_CHANGE:
                    weatherActor.tell(command, getSelf());
                    break;
                default:
                    System.out.println("Unknown command type: " + command.getType());
            }
        } catch (Exception e) {
            System.err.println("Failed to process MQTT message: " + e.getMessage());
        }
    }
}
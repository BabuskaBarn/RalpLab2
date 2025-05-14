package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.mqtt.MqttService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {
    public interface BlindsCommand {}

    public static final class WeatherChanged implements BlindsCommand {
        final boolean isSunny;
        public WeatherChanged(boolean isSunny) {
            this.isSunny = isSunny;
        }
    }

    public static final class MediaStatusChanged implements BlindsCommand {
        final boolean isPlaying;
        public MediaStatusChanged(boolean isPlaying) {
            this.isPlaying = isPlaying;
        }
    }

    private boolean isOpen = true;
    private boolean moviePlaying = false;
    private final MqttService mqttService;

    public static Behavior<BlindsCommand> create(MqttService mqttService) {
        return Behaviors.setup(context -> new Blinds(context, mqttService));
    }

    private Blinds(ActorContext<BlindsCommand> context, MqttService mqttService) {
        super(context);
        this.mqttService = mqttService;

        // Subscribe to MQTT media updates
        try {
            mqttService.subscribe("home/media", (topic, message) -> {
                ObjectMapper mapper = new ObjectMapper();
                MediaStatus status = mapper.readValue(message.getPayload(), MediaStatus.class);
                getContext().getSelf().tell(new MediaStatusChanged(status.isPlaying));
            });
        } catch (Exception e) {
            getContext().getLog().error("Failed to subscribe to MQTT", e);
        }

        getContext().getLog().info("Blinds started");
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeatherChanged.class, this::onWeatherChanged)
                .onMessage(MediaStatusChanged.class, this::onMediaStatusChanged)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindsCommand> onWeatherChanged(WeatherChanged w) {
        if (!moviePlaying) { // Only react to weather if no movie is playing
            boolean shouldOpen = !w.isSunny;
            setPosition(shouldOpen);
        }
        return this;
    }

    private Behavior<BlindsCommand> onMediaStatusChanged(MediaStatusChanged m) {
        this.moviePlaying = m.isPlaying;
        setPosition(!moviePlaying); // Close if movie playing, open otherwise
        return this;
    }

    private void setPosition(boolean open) {
        this.isOpen = open;
        getContext().getLog().info("Blinds are now {}", isOpen ? "open" : "closed");
    }

    private Blinds onPostStop() {
        getContext().getLog().info("Blinds actor stopped");
        return this;
    }

    private static class MediaStatus {
        public boolean isPlaying;
        public MediaStatus(boolean playing) { this.isPlaying = playing; }
    }
}
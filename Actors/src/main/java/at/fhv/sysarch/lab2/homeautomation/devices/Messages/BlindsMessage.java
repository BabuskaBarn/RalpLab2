package at.fhv.sysarch.lab2.homeautomation.devices.Messages;
import akka.actor.typed.ActorRef;

public interface BlindsMessage {
    // Command to get current status (with reply-to reference)
    final class GetStatus implements BlindsMessage {
        public final ActorRef<StatusResponse> replyTo;

        public GetStatus(ActorRef<StatusResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    // Status response
    final class StatusResponse implements BlindsMessage {
        public final boolean isOpen;
        public final boolean moviePlaying;

        public StatusResponse(boolean isOpen, boolean moviePlaying) {
            this.isOpen = isOpen;
            this.moviePlaying = moviePlaying;
        }
    }

    // Command to set blinds position
    final class SetBlindsPosition implements BlindsMessage {
        public final boolean shouldOpen;

        public SetBlindsPosition(boolean shouldOpen) {
            this.shouldOpen = shouldOpen;
        }
    }

    // Command to update movie playing state
    final class SetMoviePlaying implements BlindsMessage {
        public final boolean isPlaying;

        public SetMoviePlaying(boolean isPlaying) {
            this.isPlaying = isPlaying;
        }
    }

    // Command to update based on weather
    final class WeatherUpdate implements BlindsMessage {
        public final String weatherCondition;

        public WeatherUpdate(String weatherCondition) {
            this.weatherCondition = weatherCondition;
        }
    }
}
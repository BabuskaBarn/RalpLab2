package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.BlindsMessage;

public class Blinds extends AbstractBehavior<BlindsMessage> {
    private boolean isOpen;
    private boolean moviePlaying;

    public static Behavior<BlindsMessage> create() {
        return Behaviors.setup(Blinds::new);
    }

    private Blinds(ActorContext<BlindsMessage> context) {
        super(context);
        this.isOpen = true;  // Default to open
        this.moviePlaying = false;
        context.getLog().info("Blinds actor started");
    }

    @Override
    public Receive<BlindsMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(BlindsMessage.GetStatus.class, this::onGetStatus)
                .onMessage(BlindsMessage.SetBlindsPosition.class, this::onSetPosition)
                .onMessage(BlindsMessage.SetMoviePlaying.class, this::onSetMoviePlaying)
                .onMessage(BlindsMessage.WeatherUpdate.class, this::onWeatherUpdate)
                .build();
    }

    private Behavior<BlindsMessage> onGetStatus(BlindsMessage.GetStatus msg) {
        getContext().getLog().info("Received status request");
        msg.replyTo.tell(new BlindsMessage.StatusResponse(isOpen, moviePlaying));
        return this;
    }

    private Behavior<BlindsMessage> onSetPosition(BlindsMessage.SetBlindsPosition msg) {
        this.isOpen = msg.shouldOpen;
        getContext().getLog().info("Blinds {}", isOpen ? "opened" : "closed");
        return this;
    }

    private Behavior<BlindsMessage> onSetMoviePlaying(BlindsMessage.SetMoviePlaying msg) {
        this.moviePlaying = msg.isPlaying;
        getContext().getLog().info("Movie playing state updated to {}", moviePlaying);

        // Close blinds if movie starts, but don't open if it stops (weather will handle that)
        if (moviePlaying && isOpen) {
            getContext().getSelf().tell(new BlindsMessage.SetBlindsPosition(false));
        }
        return this;
    }

    private Behavior<BlindsMessage> onWeatherUpdate(BlindsMessage.WeatherUpdate msg) {
        if (!moviePlaying) {  // Only react to weather if no movie is playing
            boolean shouldOpen = !"sunny".equals(msg.weatherCondition);
            if (isOpen != shouldOpen) {
                getContext().getSelf().tell(new BlindsMessage.SetBlindsPosition(shouldOpen));
            }
        }
        return this;
    }
}
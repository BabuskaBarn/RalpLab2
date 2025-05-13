package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.MediaMessage;

public class MediaStation extends AbstractBehavior<MediaMessage> {  // Changed to MediaMessage interface
    private boolean isMoviePlaying;
    private String currentMovie;

    public static Behavior<MediaMessage> create() {  // Changed return type
        return Behaviors.setup(MediaStation::new);
    }

    private MediaStation(ActorContext<MediaMessage> context) {  // Changed context type
        super(context);
        this.currentMovie = null;
        this.isMoviePlaying = false;
    }

    @Override
    public Receive<MediaMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(MediaMessage.PlayMovie.class, this::onPlayMovie)
                .onMessage(MediaMessage.StopMovie.class, this::onStopMovie)
                .onMessage(MediaMessage.GetStatus.class, this::onGetStatus)
                .build();
    }

    private Behavior<MediaMessage> onPlayMovie(MediaMessage.PlayMovie msg) {  // Changed return type
        if (isMoviePlaying) {
            getContext().getLog().info("Cannot play {} - {} is already playing",
                    msg.movieTitle, currentMovie);
        } else {
            this.currentMovie = msg.movieTitle;
            this.isMoviePlaying = true;
            getContext().getLog().info("Now playing: {}", currentMovie);
        }
        return this;
    }

    private Behavior<MediaMessage> onStopMovie(MediaMessage.StopMovie msg) {  // Changed return type
        if (isMoviePlaying) {
            getContext().getLog().info("Stopping movie: {}", currentMovie);
            this.isMoviePlaying = false;
            this.currentMovie = null;
        } else {
            getContext().getLog().info("No movie is currently playing");
        }
        return this;
    }

    private Behavior<MediaMessage> onGetStatus(MediaMessage.GetStatus msg) {  // Changed return type
        getContext().getLog().info("Media Station is {}",
                isMoviePlaying ? "playing " + currentMovie : "idle");
        return this;
    }

    public boolean isPlaying() {
        return isMoviePlaying;
    }
}
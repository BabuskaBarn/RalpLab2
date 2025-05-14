package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.states.MovieState;

public class MediaStation extends AbstractBehavior<MediaStation.MediaStationCommand> {

    // Command Interface für MediaStation
    public interface MediaStationCommand {}

    // PlayMovie Command
    public static final class PlayMovie implements MediaStationCommand {
        public final MovieState movieState;

        public PlayMovie(MovieState movieState) {
            this.movieState = movieState;
        }
    }

    // StopMovie Command
    public static final class StopMovie implements MediaStationCommand {
        public StopMovie() {}
    }

    // GetStatus Command
    public static final class GetStatus implements MediaStationCommand {
        public GetStatus() {}
    }

    private MovieState currentMovie;
    private final ActorRef<Blinds.BlindsCommand> blinds;

    // Factory-Methode für die Erstellung des Actors
    public static Behavior<MediaStationCommand> create(ActorRef<Blinds.BlindsCommand> blinds) {
        return Behaviors.setup(context -> new MediaStation(context, blinds));
    }

    // Konstruktor
    private MediaStation(ActorContext<MediaStationCommand> context, ActorRef<Blinds.BlindsCommand> blinds) {
        super(context);
        this.currentMovie = null;
        this.blinds = blinds;
    }

    @Override
    public Receive<MediaStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(PlayMovie.class, this::onPlayMovie)
                .onMessage(StopMovie.class, this::onStopMovie)
                .onMessage(GetStatus.class, this::onGetStatus)
                .build();
    }

    // Verarbeitung der PlayMovie-Nachricht
    private Behavior<MediaStationCommand> onPlayMovie(PlayMovie msg) {
        // Wenn bereits ein Film läuft, kann kein weiterer gestartet werden
        if (currentMovie != null) {
            getContext().getLog().warn(
                    "Cannot start {}: Movie {} is already playing",
                    msg.movieState.getName(), currentMovie.getName()
            );
            return this;  // Keine Änderung des Zustands
        }

        currentMovie = msg.movieState;
        getContext().getLog().info("Now playing: {}", currentMovie.getName());

        // Blinds benachrichtigen, dass ein Film läuft
        blinds.tell(new Blinds.MediaStatusChanged(true));

        return this;
    }

    // Verarbeitung der StopMovie-Nachricht
    private Behavior<MediaStationCommand> onStopMovie(StopMovie msg) {
        if (currentMovie != null) {
            getContext().getLog().info("Stopping movie: {}", currentMovie.getName());
            currentMovie = null;

            // Blinds benachrichtigen, dass kein Film mehr läuft
            blinds.tell(new Blinds.MediaStatusChanged(false));
        } else {
            getContext().getLog().info("No movie is currently playing");
        }
        return this;
    }

    // Verarbeitung der GetStatus-Nachricht
    private Behavior<MediaStationCommand> onGetStatus(GetStatus msg) {
        getContext().getLog().info("Media Station is {}",
                currentMovie != null ? "playing " + currentMovie.getName() : "idle");
        return this;
    }

    // Getter für den aktuellen Filmstatus
    public boolean isPlaying() {
        return currentMovie != null;
    }

    // Getter für den aktuellen Film
    public MovieState getCurrentMovie() {
        return currentMovie;
    }
}

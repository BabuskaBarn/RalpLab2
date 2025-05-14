package at.fhv.sysarch.lab2.homeautomation.devices.Messages;




import at.fhv.sysarch.lab2.homeautomation.devices.states.MovieState;

public interface MediaMessage {
    public static final class PlayMovie implements MediaMessage {
        public final MovieState movieState;

        public PlayMovie(MovieState movieState) {
            this.movieState = movieState;
        }
    }

    public static final class StopMovie implements MediaMessage {}

    public static final class GetStatus implements MediaMessage {}
}

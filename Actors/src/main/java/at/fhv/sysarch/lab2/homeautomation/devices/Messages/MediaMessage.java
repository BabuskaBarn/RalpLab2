package at.fhv.sysarch.lab2.homeautomation.devices.Messages;

public interface MediaMessage {
    public static class PlayMovie implements MediaMessage {
        public final String movieTitle;
        public PlayMovie(String movieTitle) {
            this.movieTitle = movieTitle;
        }
    }

    public static class StopMovie implements MediaMessage {}

    public static class GetStatus implements MediaMessage {}

    public static class Status implements MediaMessage {
        public final boolean isPlaying;
        public final String currentMovie;
        public Status(boolean isPlaying, String currentMovie) {
            this.isPlaying = isPlaying;
            this.currentMovie = currentMovie;
        }
    }
}
package at.fhv.sysarch.lab2.homeautomation.devices.states;

import java.time.Duration;

public enum MovieState {
    MOVIE1("Movie1", Duration.ofMinutes(120)),
    MOVIE2("Movie2", Duration.ofMinutes(90)),
    MOVIE3("Movie3", Duration.ofMinutes(120));

    private final String name;

    private Duration duration;

    MovieState(String name, Duration duration) {
        this.name = name;
        this.duration = duration;
    }


    public Duration getDuration() {
        return duration;
    }


    public String getName() {
        return name;
    }
}

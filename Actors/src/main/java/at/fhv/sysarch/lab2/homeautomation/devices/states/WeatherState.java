package at.fhv.sysarch.lab2.homeautomation.devices.states;

public enum WeatherState {
    SUNNY("sunny"),
    STORM("storm"),
    FOGGY("foggy"),
    CLOUDY("cloudy"),
    RAIN("rain"),
    SNOW("snow");
    private final String weatherState;

    WeatherState(String weatherState) {
        this.weatherState = weatherState;
    }

    public String getWeatherState() {
        return weatherState;
    }
}


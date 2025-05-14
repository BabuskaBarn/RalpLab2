package at.fhv.sysarch.lab2.homeautomation.devices.states;

public enum WeatherState {

    SUNNY("sunny"),
    STORMY("stormy"),
    FOGGY("foggy"),
    CLOUDY("cloudy");

    private String weatherState;
    WeatherState(String weatherState) {
        this.weatherState = weatherState;
    }

    public String getWeatherState() {
        return weatherState;
    }

}


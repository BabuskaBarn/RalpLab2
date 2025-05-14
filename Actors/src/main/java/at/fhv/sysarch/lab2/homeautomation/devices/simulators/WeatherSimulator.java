package at.fhv.sysarch.lab2.homeautomation.devices.simulators;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment;
import at.fhv.sysarch.lab2.homeautomation.devices.states.WeatherState;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;


import java.time.Duration;
import java.util.Random;

public class WeatherSimulator extends AbstractBehavior<Environment.EnvironmentCommand> {

    private WeatherState weatherState = WeatherState.SUNNY;
    private final TimerScheduler<Environment.EnvironmentCommand> timer;

    public static Behavior<Environment.EnvironmentCommand> create() {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new WeatherSimulator(ctx, timers))
        );
    }

    private WeatherSimulator(ActorContext<Environment.EnvironmentCommand> context,
                             TimerScheduler<Environment.EnvironmentCommand> timer) {
        super(context);
        this.timer = timer;
        timer.startTimerAtFixedRate(new Environment.WeatherConditionsChanger(), Duration.ofSeconds(35));
    }

    @Override
    public Receive<Environment.EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Environment.WeatherConditionsChanger.class, this::onWeatherChange)
                .onMessage(Environment.SetWeather.class, this::onSetWeather)
                .onMessage(Environment.ReadWeather.class, this::onReadWeather)
                .build();
    }

    private Behavior<Environment.EnvironmentCommand> onWeatherChange(Environment.WeatherConditionsChanger msg) {
        weatherState = WeatherState.values()[new Random().nextInt(WeatherState.values().length)];
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onSetWeather(Environment.SetWeather msg) {
        weatherState = msg.weatherState;
        return this;
    }
        //wieder flasche logik aber machbar
        public static final class ForwardWeatherCommand implements WeatherSensor.WeatherCommand {
            private final WeatherState weatherState;

            public ForwardWeatherCommand(WeatherState weatherState) {
                this.weatherState = weatherState;
            }

            public WeatherState getWeatherState() {
                return weatherState;
            }
        }

}

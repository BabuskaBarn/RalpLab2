package at.fhv.sysarch.lab2.homeautomation.devices.simulators;

import akka.actor.typed.ActorRef;
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
    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;

    public static Behavior<Environment.EnvironmentCommand> create(ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new WeatherSimulator(ctx, timers, weatherSensor))
        );
    }

    private WeatherSimulator(ActorContext<Environment.EnvironmentCommand> context,
                             TimerScheduler<Environment.EnvironmentCommand> timer,
                             ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
        super(context);
        this.timer = timer;
        this.weatherSensor = weatherSensor;

        // Simulation startet NICHT automatisch – nur wenn aktiviert
    }

    private boolean simulationActive = false;

    @Override
    public Receive<Environment.EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Environment.WeatherConditionsChanger.class, this::onWeatherChange)
                .onMessage(Environment.SetWeather.class, this::onSetWeather)
                .onMessage(Environment.ReadWeather.class, this::onReadWeather)
                .onMessage(Environment.ActivateSimulation.class, this::onActivateSimulation)
                .onMessage(Environment.DeactivateSimulation.class, this::onDeactivateSimulation)
                .build();
    }

    private Behavior<Environment.EnvironmentCommand> onActivateSimulation(Environment.ActivateSimulation msg) {
        simulationActive = true;
        timer.startTimerAtFixedRate(new Environment.WeatherConditionsChanger(), Duration.ofSeconds(10));
        getContext().getLog().info("WeatherSimulator activated");
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onDeactivateSimulation(Environment.DeactivateSimulation msg) {
        simulationActive = false;
        timer.cancel(Environment.WeatherConditionsChanger.class);
        getContext().getLog().info("WeatherSimulator deactivated");
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onWeatherChange(Environment.WeatherConditionsChanger msg) {
        if (!simulationActive) return this;

        weatherState = WeatherState.values()[new Random().nextInt(WeatherState.values().length)];
        weatherSensor.tell(new ForwardWeatherCommand(weatherState));
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onSetWeather(Environment.SetWeather msg) {
        weatherState = msg.weatherState;
        weatherSensor.tell(new ForwardWeatherCommand(weatherState));
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onReadWeather(Environment.ReadWeather readWeather) {
        weatherSensor.tell(new ForwardWeatherCommand(weatherState));
        return this;
    }

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


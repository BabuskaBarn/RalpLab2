package at.fhv.sysarch.lab2.homeautomation.devices.Messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

public class Enviornment extends AbstractBehavior<Environment.EnvironmentCommand> {

    public Enviornment(ActorContext<Environment.EnvironmentCommand> context) {
        super(context);
    }

    @Override
    public Receive<Environment.EnvironmentCommand> createReceive() {
        return null;
    }

    public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

        public interface EnvironmentCommand {}

        public static final class TemperatureChanger implements EnvironmentCommand {

        }

        public static final class WeatherConditionsChanger implements EnvironmentCommand {
            final Optional<WeatherState> _weatherState;

            public WeatherConditionsChanger(Optional<WeatherState> weatherState) {
                this._weatherState = weatherState;
            }
        }

        public static final class ReadWeather implements EnvironmentCommand {
            private final ActorRef<WeatherSensor.WeatherCommand> sender;

            public ReadWeather(ActorRef<WeatherSensor.WeatherCommand> sender) {
                this.sender = sender;
            }

            public ActorRef<WeatherSensor.WeatherCommand> getSender() {
                return sender;
            }
        }

        public static final class ReadTemperature implements EnvironmentCommand {
            private final ActorRef<TemperatureSensor.TemperatureCommand> sender;

            public ReadTemperature(ActorRef<TemperatureSensor.TemperatureCommand> sender) {
                this.sender = sender;
            }

            public ActorRef<TemperatureSensor.TemperatureCommand> getSender() {
                return sender;
            }

        }
        private double _temperature = 26;
        private WeatherState _weatherState = WeatherState.SUNNY;

        private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
        private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;

        // Provide the means for manually setting the temperature
        public static final class SetTemperature implements  EnvironmentCommand {
            double _temperature;

            public SetTemperature(double temperature){
                _temperature = temperature;
            }
        }

        private Behavior<EnvironmentCommand> onSetTemperature(SetTemperature command) {
            _temperature = command._temperature;
            return this;
        }

        private Behavior<EnvironmentCommand> onReadTemperature(ReadTemperature command) {
            command.getSender().tell(new TemperatureSensor.ForwardTemperature(Optional.of(_temperature)));
            return this;
        }

        private Behavior<EnvironmentCommand> onReadWeather(ReadWeather command) {
            command.getSender().tell(new WeatherSensor.ForwardWeatherCommand(_weatherState));
            return this;
        }
        // Provide the means for manually setting the weather
        public static final class SetWeather implements  EnvironmentCommand {
            WeatherState _weatherState;

            public SetWeather(WeatherState weatherState){
                _weatherState = weatherState;
            }
        }

        private Behavior<EnvironmentCommand> onSetWeather(SetWeather command) {
            _weatherState = command._weatherState;
            return this;
        }

        public static Behavior<EnvironmentCommand> create(){
            return Behaviors.setup(context ->  Behaviors.withTimers(timers -> new Environment(context, timers, timers)));
        }

        private Environment(ActorContext<EnvironmentCommand> context,TimerScheduler<EnvironmentCommand> tempTimer, TimerScheduler<EnvironmentCommand> weatherTimer) {
            super(context);
            this.temperatureTimeScheduler = tempTimer;
            this.weatherTimeScheduler = weatherTimer;
            this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanger(), Duration.ofSeconds(5));
            this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherConditionsChanger(Optional.of(_weatherState)), Duration.ofSeconds(35));
        }

        @Override
        public Receive<EnvironmentCommand> createReceive() {
            return newReceiveBuilder()
                    .onMessage(TemperatureChanger.class, this::onChangeTemperature)
                    .onMessage(WeatherConditionsChanger.class, this::onChangeWeather)
                    .onMessage(SetTemperature.class, this::onSetTemperature)
                    .onMessage(SetWeather.class, this::onSetWeather)
                    .onMessage(ReadTemperature.class, this::onReadTemperature)
                    .onMessage(ReadWeather.class, this::onReadWeather)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        }

        private static double getRandomTemperatureChange() {
            return new Random().nextDouble(-1, 1);
        }

        private Behavior<EnvironmentCommand> onChangeTemperature(TemperatureChanger t) {
            // Implement behavior for random changes to temperature
            this._temperature += getRandomTemperatureChange();

            // TODO: Handling of temperature change. Are sensors notified or do they read the temperature?
            return this;
        }

        private static WeatherState getRandomWeatherState() {
            WeatherState[] weatherStates = WeatherState.values();
            return weatherStates[new Random().nextInt(weatherStates.length)];
        }

        private Behavior<EnvironmentCommand> onChangeWeather(WeatherConditionsChanger w) {
            // Implement behavior for random changes to weather. Include more than just sunny and not sunny
            _weatherState = getRandomWeatherState();


            // TODO: Handling of weather change. Are sensors notified or do they read the weather information?
            return this;
        }


        private Environment onPostStop(){
            getContext().getLog().info("Environment actor stopped");
            return this;
        }
    }



}

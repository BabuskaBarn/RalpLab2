package at.fhv.sysarch.lab2.homeautomation.devices.Messages;

import akka.actor.typed.ActorRef;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.states.WeatherState;

public class Environment {
        //Sozusagen ein Interface mit dem die Simulatoren arbeiten
        public interface EnvironmentCommand {}

        public static final class ReadTemperature implements at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment.EnvironmentCommand {
            private final ActorRef<TemperatureSensor.TemperatureCommand> sender;

            public ReadTemperature(ActorRef<TemperatureSensor.TemperatureCommand> sender) {
                this.sender = sender;
            }

            public ActorRef<TemperatureSensor.TemperatureCommand> getSender() {
                return sender;
            }
        }

        public static final class SetTemperature implements at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment.EnvironmentCommand {
            public final double temperature;

            public SetTemperature(double temperature) {
                this.temperature = temperature;
            }
        }

        public static final class ReadWeather implements at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment.EnvironmentCommand {
            private final ActorRef<WeatherSensor.WeatherCommand> sender;

            public ReadWeather(ActorRef<WeatherSensor.WeatherCommand> sender) {
                this.sender = sender;
            }

            public ActorRef<WeatherSensor.WeatherCommand> getSender() {
                return sender;
            }
        }

        public static final class SetWeather implements at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment.EnvironmentCommand {
            public final WeatherState weatherState;

            public SetWeather(WeatherState weatherState) {
                this.weatherState = weatherState;
            }
        }

        public static final class TemperatureChanger implements at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment.EnvironmentCommand {}

        public static final class WeatherConditionsChanger implements at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment.EnvironmentCommand {}
    }

}

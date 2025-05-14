package at.fhv.sysarch.lab2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import at.fhv.sysarch.lab2.homeautomation.HomeAutomationController;
import at.fhv.sysarch.lab2.homeautomation.devices.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.devices.Blinds;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.mqtt.MqttService;

public class HomeAutomationSystem {

    public static void main(String[] args) {
        MqttService mqttService = new MqttService("tcp://localhost:1883", "home-automation");

        // Actor System erstellen
        ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "homeAutomation");

        // Geräte erstellen
        ActorRef<AirCondition.AirConditionCommand> airCondition =
                system.systemActorOf(AirCondition.create(), "airCondition");

        ActorRef<Blinds.BlindsCommand> blinds =
                system.systemActorOf(Blinds.create(mqttService), "blinds");

        ActorRef<TemperatureSensor.TemperatureCommand> tempSensor =
                system.systemActorOf(TemperatureSensor.create(airCondition, mqttService), "tempSensor");

        ActorRef<WeatherSensor.WeatherCommand> weatherSensor =
                system.systemActorOf(WeatherSensor.create(blinds, mqttService), "weatherSensor");

        // System laufen lassen
        Thread.currentThread().join();

    }


}
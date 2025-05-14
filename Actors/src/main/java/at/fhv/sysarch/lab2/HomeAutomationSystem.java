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
        ActorSystem<Void> home = ActorSystem.create(HomeAutomationController.create(), "HomeAutomation");

    }


}
package at.fhv.sysarch.lab2.homeautomation;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.devices.Fridge.Fridge;
import at.fhv.sysarch.lab2.homeautomation.devices.MediaStation.MediaStationCommand;
import at.fhv.sysarch.lab2.homeautomation.mqtt.MqttSubscriber;
import at.fhv.sysarch.lab2.ordersystem.internal.OrderProcessor;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;
import at.fhv.sysarch.lab2.ordersystem.internal.OrderProcessor;
import at.fhv.sysarch.lab2.ordersystem.internal.OrderProcessorWithResults;

import java.util.UUID;

public class HomeAutomationController extends AbstractBehavior<Void> {

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }


    private HomeAutomationController(ActorContext<Void> context) {
        super(context);

        // Geräte
        ActorRef<AirCondition.AirConditionCommand> airCondition =
                context.spawn(AirCondition.create(UUID.randomUUID().toString()), "AirCondition");

        ActorRef<OrderProcessor.OrderCommand> orderProcessor =
                context.spawn(OrderProcessorWithResults.create(), "OrderProcessor");

        ActorRef<Fridge.FridgeCommand> fridge =
                context.spawn(Fridge.create(), "Fridge");

        // Zentrale: Blinds
        ActorRef<Blinds.BlindsCommand> blinds =
                context.spawn(Blinds.create(), "Blinds");

        // Sensoren
        ActorRef<WeatherSensor.WeatherCommand> weatherSensor =
                context.spawn(WeatherSensor.create(blinds), "WeatherSensor");

        ActorRef<TemperatureSensor.TemperatureCommand> tempSensor =
                context.spawn(TemperatureSensor.create(airCondition), "TemperatureSensor");

        // MQTT Subscriber: sendet Werte an WeatherSensor + TemperatureSensor
        context.spawn(MqttSubscriber.create(weatherSensor, tempSensor), "MqttSubscriber");

        // Media Station: sendet MediaStatus an Blinds
        ActorRef<MediaStationCommand> mediaStation =
                context.spawn(MediaStation.create(blinds), "MediaStation");

        // UI (muss auch Media & Weather simulieren können)
        ActorRef<Void> ui =
                context.spawn(UI.create(
                        mediaStation,
                        airCondition,
                        blinds,
                        tempSensor,
                        weatherSensor,
                        fridge), "UI");

        context.getLog().info("HomeAutomation Application started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder()
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private HomeAutomationController onPostStop() {
        getContext().getLog().info("HomeAutomation Application stopped");
        return this;
    }
}

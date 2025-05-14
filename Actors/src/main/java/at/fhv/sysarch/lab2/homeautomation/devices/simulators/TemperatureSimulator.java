package at.fhv.sysarch.lab2.homeautomation.devices.simulators;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

public class TemperatureSimulator extends AbstractBehavior<Environment.EnvironmentCommand> { {
}

    private double temperature = 25.0;
    private final TimerScheduler<Environment.EnvironmentCommand> timer;

    public static Behavior<Environment.EnvironmentCommand> create() {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new TemperatureSimulator(ctx, timers))
        );
    }

    private TemperatureSimulator(ActorContext<Environment.EnvironmentCommand> context,
                                 TimerScheduler<Environment.EnvironmentCommand> timer) {
        super(context);
        this.timer = timer;
        timer.startTimerAtFixedRate(new Environment.TemperatureChanger(), Duration.ofSeconds(5));
    }

    @Override
    public Receive<Environment.EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Environment.TemperatureChanger.class, this::onTemperatureChange)
                .onMessage(Environment.SetTemperature.class, this::onSetTemperature)
                .onMessage(Environment.ReadTemperature.class, this::onReadTemperature)
                .build();
    }

    private Behavior<Environment.EnvironmentCommand> onTemperatureChange(Environment.TemperatureChanger msg) {
        temperature += new Random().nextDouble(-1.0, 1.0);
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onSetTemperature(Environment.SetTemperature msg) {
        temperature = msg.temperature;
        return this;
    }
        //Falsche logik mit neu temperatureSensor
    private Behavior<Environment.EnvironmentCommand> onReadTemperature(Environment.ReadTemperature msg) {
        msg.getSender().tell(new TemperatureSensor.ForwardTemperature(Optional.of(temperature)));
        return this;
    }
}




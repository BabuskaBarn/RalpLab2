package at.fhv.sysarch.lab2.homeautomation.devices.simulators;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;

import java.time.Duration;
import java.util.Random;

public class TemperatureSimulator extends AbstractBehavior<Environment.EnvironmentCommand> {

    private double temperature = 25.0;
    private final TimerScheduler<Environment.EnvironmentCommand> timer;
    private final ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;
    private boolean simulationActive = false;

    public static Behavior<Environment.EnvironmentCommand> create(ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor) {
        return Behaviors.setup(ctx ->
                Behaviors.withTimers(timers -> new TemperatureSimulator(ctx, timers, temperatureSensor))
        );
    }

    private TemperatureSimulator(ActorContext<Environment.EnvironmentCommand> context,
                                 TimerScheduler<Environment.EnvironmentCommand> timer,
                                 ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor) {
        super(context);
        this.timer = timer;
        this.temperatureSensor = temperatureSensor;
    }

    @Override
    public Receive<Environment.EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Environment.ActivateSimulation.class, this::onActivateSimulation)
                .onMessage(Environment.DeactivateSimulation.class, this::onDeactivateSimulation)
                .onMessage(Environment.TemperatureChanger.class, this::onTemperatureChange)
                .onMessage(Environment.SetTemperature.class, this::onSetTemperature)
                .onMessage(Environment.ReadTemperature.class, this::onReadTemperature)
                .build();
    }

    private Behavior<Environment.EnvironmentCommand> onActivateSimulation(Environment.ActivateSimulation msg) {
        simulationActive = true;
        timer.startTimerAtFixedRate(new Environment.TemperatureChanger(), Duration.ofSeconds(5));
        getContext().getLog().info("TemperatureSimulator activated");
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onDeactivateSimulation(Environment.DeactivateSimulation msg) {
        simulationActive = false;
        timer.cancel(Environment.TemperatureChanger.class);
        getContext().getLog().info("TemperatureSimulator deactivated");
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onTemperatureChange(Environment.TemperatureChanger msg) {
        if (!simulationActive) return this;

        temperature += new Random().nextDouble(-1.0, 1.0);
        temperatureSensor.tell(new ForwardTemperatureCommand(temperature));
        getContext().getLog().info("Simulated temperature: {}", temperature);
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onSetTemperature(Environment.SetTemperature msg) {
        this.temperature = msg.temperature;
        temperatureSensor.tell(new ForwardTemperatureCommand(temperature));
        return this;
    }

    private Behavior<Environment.EnvironmentCommand> onReadTemperature(Environment.ReadTemperature msg) {
        temperatureSensor.tell(new ForwardTemperatureCommand(temperature));
        return this;
    }

    // ========== Interne Nachricht an den TemperatureSensor ==========
    public static final class ForwardTemperatureCommand implements TemperatureSensor.TemperatureCommand {
        private final double temperature;

        public ForwardTemperatureCommand(double temperature) {
            this.temperature = temperature;
        }

        public double getTemperature() {
            return temperature;
        }
    }
}

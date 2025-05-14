package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.WeatherConditionMessage;

public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {

    public interface BlindsCommand {}

    // Kommando zur Verarbeitung der Wetteränderung
    public static final class WeatherChanged implements BlindsCommand {
        public final boolean isSunny;

        public WeatherChanged(boolean isSunny) {
            this.isSunny = isSunny;
        }
    }

    // Kommando zur Verarbeitung der Medienstatusänderung
    public static final class MediaStatusChanged implements BlindsCommand {
        public final boolean isPlaying;

        public MediaStatusChanged(boolean isPlaying) {
            this.isPlaying = isPlaying;
        }
    }

    // Kommando zum manuellen Umschalten der Jalousien
    public static final class ToggleCommand implements BlindsCommand {}

    private boolean isOpen = true;
    private boolean isMoviePlaying = false;
    private boolean isSunny = true;

    // Factory-Methode zum Erstellen des Blinds-Actors
    public static Behavior<BlindsCommand> create() {
        return Behaviors.setup(Blinds::new);
    }

    // Konstruktor
    private Blinds(ActorContext<BlindsCommand> context) {
        super(context);
        context.getLog().info("Blinds actor started");
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeatherChanged.class, this::onWeatherChanged)
                .onMessage(MediaStatusChanged.class, this::onMediaStatusChanged)
                .onMessage(ToggleCommand.class, this::onToggle)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    // Reaktion auf Wetteränderung
    private Behavior<BlindsCommand> onWeatherChanged(WeatherChanged w) {
        this.isSunny = w.isSunny;
        evaluatePosition();
        return this;
    }

    // Reaktion auf Medienstatusänderung
    private Behavior<BlindsCommand> onMediaStatusChanged(MediaStatusChanged m) {
        this.isMoviePlaying = m.isPlaying;
        evaluatePosition();
        return this;
    }

    // Reaktion auf manuelles Umschalten
    private Behavior<BlindsCommand> onToggle(ToggleCommand cmd) {
        setPosition(!isOpen);
        return this;
    }

    // Logik zum automatischen Steuern der Blinds
    private void evaluatePosition() {
        if (isMoviePlaying) {
            setPosition(false); // Schließen bei laufendem Film
        } else {
            setPosition(!isSunny); // Öffnen, wenn es nicht sonnig ist
        }
    }

    // Setzt die aktuelle Position der Jalousien
    private void setPosition(boolean open) {
        if (this.isOpen != open) {
            this.isOpen = open;
            getContext().getLog().info("Blinds are now {}", isOpen ? "open" : "closed");
        }
    }

    // Verhalten bei Stoppen des Actors
    private Blinds onPostStop() {
        getContext().getLog().info("Blinds actor stopped");
        return this;
    }
}

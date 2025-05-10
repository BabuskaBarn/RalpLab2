package at.fhv.sysarch.lab2.homeautomation.handler;

import akka.actor.ActorContext;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Scanner;

public class InputHandler  extends AbstractBehavior<InputHandler.Command> {
    public interface Command {}

    public static final class StartInputLoop implements Command {
        public final ActorRef<UI.UserInput> uiActor;
        public StartInputLoop(ActorRef<UI.UserInput> uiActor) {
            this.uiActor = uiActor;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(InputHandler::new);
    }

    private InputHandler(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartInputLoop.class, this::onStartInputLoop)
                .build();
    }

    private Behavior<Command> onStartInputLoop(StartInputLoop msg) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            String line;
            while (!(line = scanner.nextLine()).equalsIgnoreCase("quit")) {
                msg.uiActor.tell(new UI.UserInput(line));
            }
        }).start();
        return this;
    }
}



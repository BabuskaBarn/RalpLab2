package at.fhv.sysarch.lab2.homeautomation.handler;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

import java.util.Scanner;

public class InputHandler extends AbstractBehavior<InputHandler.Command> {
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
        getContext().getLog().info("Starting input loop...");

        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("System ready. Type 'help' for commands or 'quit' to exit.");

            while (true) {
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("quit")) {
                    getContext().getLog().info("Shutting down input handler");
                    break;
                }
                msg.uiActor.tell(new UI.RawInput(line));
            }
            scanner.close();
        }).start();

        return this;
    }
}
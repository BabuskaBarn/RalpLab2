package at.fhv.sysarch.lab2.ordersystem;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import at.fhv.sysarch.lab2.ordersystem.internal.OrderProcessorWithResults;

public class OrderSystemController {

    public static Behavior<Void> create() {
        return Behaviors.setup(context -> {
            // Startet den OrderProcessor
            context.spawn(OrderProcessorWithResults.create(), "orderProcessor");

            return Behaviors.empty(); // Kein Verhalten nötig
        });
    }
}

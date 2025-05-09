package at.fhv.sysarch.lab2;

import akka.actor.typed.ActorSystem;
import at.fhv.sysarch.lab2.ordersystem.OrderSystemController;

public class OrderSystem {
    public static void main(String[] args) {
        ActorSystem<Void> home = ActorSystem.create(OrderSystemController.create(), "OrderSystem");
    }
}

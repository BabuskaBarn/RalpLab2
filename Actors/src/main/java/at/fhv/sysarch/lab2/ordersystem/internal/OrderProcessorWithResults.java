package at.fhv.sysarch.lab2.ordersystem.internal;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class OrderProcessorWithResults {

        public static Behavior<OrderProcessor.OrderCommand> create() {
            return Behaviors.setup(context -> {
                ActorRef<OrderProcessor.OrderCommand> handler =
                        context.spawn(OrderHandler.create(null), "orderHandler");

                ActorRef<OrderProcessor.OrderCommand> service =
                        context.spawn(OrderServiceImpl.create(), "orderService");

                return new AbstractBehavior<OrderProcessor.OrderCommand>(context) {
                    @Override
                    public Receive<OrderProcessor.OrderCommand> createReceive() {
                        return newReceiveBuilder()
                                .onMessage(OrderProcessor.ProcessOrder.class, msg -> {
                                    handler.tell(msg);
                                    return this;
                                })
                                .onMessage(OrderProcessor.StartService.class, msg -> {
                                    service.tell(msg);
                                    return this;
                                })
                                .onMessage(OrderProcessor.StopService.class, msg -> {
                                    service.tell(msg);
                                    return this;
                                })
                                .onMessage(OrderProcessor.GetStatus.class, msg -> {
                                    service.tell(msg);
                                    return this;
                                })
                                .build();
                    }
                };
            });
        }


}

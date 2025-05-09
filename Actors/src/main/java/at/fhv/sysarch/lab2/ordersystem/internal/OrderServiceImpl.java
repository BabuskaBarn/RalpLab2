package at.fhv.sysarch.lab2.ordersystem.internal;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class OrderServiceImpl {



        public static Behavior<OrderProcessor.Command> create() {
            return Behaviors.setup(context -> new AbstractBehavior<OrderProcessor.Command>(context) {
                private boolean isActive = true;

                @Override
                public Receive<OrderProcessor.Command> createReceive() {
                    return newReceiveBuilder()
                            .onMessage(OrderProcessor.StartService.class, msg -> {
                                isActive = true;
                                context.getLog().info("Service started");
                                return this;
                            })
                            .onMessage(OrderProcessor.StopService.class, msg -> {
                                isActive = false;
                                context.getLog().info("Service stopped");
                                return this;
                            })
                            .onMessage(OrderProcessor.GetStatus.class, msg -> {
                                msg.replyTo.tell(new OrderProcessor.StatusReport(isActive, 0)); // Dummy pendingOrders
                                return this;
                            })
                            .build();
                }
            });
        }
    }



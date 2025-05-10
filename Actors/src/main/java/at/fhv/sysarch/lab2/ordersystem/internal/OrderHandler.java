package at.fhv.sysarch.lab2.ordersystem.internal;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;

public class OrderHandler {
        public static Behavior<OrderProcessor.OrderCommand> create(ActorRef<OrderProcessor.StatusResponse> serviceRef) {
            return Behaviors.withTimers(timers ->
                    Behaviors.setup(context -> new AbstractBehavior<OrderProcessor.OrderCommand>(context) {

                        private int pendingOrders = 0;

                        private static final class CompleteOrder implements OrderProcessor.OrderCommand {
                            final OrderProcessor.ProcessOrder originalOrder;

                            CompleteOrder(OrderProcessor.ProcessOrder originalOrder) {
                                this.originalOrder = originalOrder;
                            }
                        }

                        @Override
                        public Receive<OrderProcessor.OrderCommand> createReceive() {
                            return newReceiveBuilder()
                                    .onMessage(OrderProcessor.ProcessOrder.class, this::onProcessOrder)
                                    .onMessage(CompleteOrder.class, this::onCompleteOrder)
                                    .build();
                        }

                        private Behavior<OrderProcessor.OrderCommand> onProcessOrder(OrderProcessor.ProcessOrder cmd) {
                            pendingOrders++;
                            getContext().getLog().info("Handling order for {} x {}", cmd.productName, cmd.quantity);

                            // Timer key can be anything unique or null (here null because only one type used)
                            timers.startSingleTimer(
                                    null,
                                    new CompleteOrder(cmd),
                                    Duration.ofSeconds(1)
                            );

                            return this;
                        }

                        private Behavior<OrderProcessor.OrderCommand> onCompleteOrder(CompleteOrder cmd) {
                            pendingOrders--;
                            cmd.originalOrder.replyTo.tell(new OrderProcessor.OrderSuccess(
                                    "ORD-" + System.currentTimeMillis(),
                                    String.format("Processed %s x %d", cmd.originalOrder.productName, cmd.originalOrder.quantity)
                            ));
                            return this;
                        }
                    })
            );
        }




}

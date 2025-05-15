package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.fridgeComponents.Order;
import at.fhv.sysarch.lab2.homeautomation.devices.fridgeComponents.Product;
import at.fhv.sysarch.lab2.ordersystem.internal.OrderProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {

    public interface FridgeCommand {}

    public static final class IgnoreResponse implements FridgeCommand {
        private final OrderResponse response;

        public IgnoreResponse(OrderResponse response) {
            this.response = response;
        }

        public OrderResponse getResponse() {
            return response;
        }
    }

    // Commands
    public static final class ConsumeProduct implements FridgeCommand {
        public final String productName;
        public final ActorRef<OrderResponse> replyTo;

        public ConsumeProduct(String productName, ActorRef<OrderResponse> replyTo) {
            this.productName = productName;
            this.replyTo = replyTo;
        }
    }

    public static final class PlaceOrder implements FridgeCommand {
        public final Order order;
        public final ActorRef<OrderResponse> replyTo;

        public PlaceOrder(Order order, ActorRef<OrderResponse> replyTo) {
            this.order = order;
            this.replyTo = replyTo;
        }
    }

    public static final class GetInventory implements FridgeCommand {
        public final ActorRef<InventoryResponse> replyTo;

        public GetInventory(ActorRef<InventoryResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    // Responses
    public interface OrderResponse {}
    public static final class OrderSuccess implements OrderResponse {
        public final String message;
        public OrderSuccess(String message) { this.message = message; }
    }
    public static final class OrderFailed implements OrderResponse {
        public final String reason;
        public OrderFailed(String reason) { this.reason = reason; }
    }

    public static final class InventoryResponse {
        public final List<Product> products;
        public final List<Order> orderHistory;
        public InventoryResponse(List<Product> products, List<Order> orderHistory) {
            this.products = new ArrayList<>(products);
            this.orderHistory = new ArrayList<>(orderHistory);
        }
    }

    // Internal message for order processor responses
    private static final class OrderCompleted implements FridgeCommand {
        public final OrderProcessor.OrderResponse response;
        public final Order order;

        public OrderCompleted(OrderProcessor.OrderResponse response, Order order) {
            this.response = response;
            this.order = order;
        }
    }

    private final List<Product> inventory;
    private final List<Order> orderHistory;
    private final float maxWeight;
    private final int maxCapacity;
    private final ActorRef<OrderProcessor.OrderCommand> orderProcessor;

    public static Behavior<FridgeCommand> create(float maxWeight, int maxCapacity,
                                                 ActorRef<OrderProcessor.OrderCommand> orderProcessor) {
        return Behaviors.setup(ctx -> new Fridge(ctx, maxWeight, maxCapacity, orderProcessor));
    }

    private Fridge(ActorContext<FridgeCommand> context, float maxWeight, int maxCapacity,
                   ActorRef<OrderProcessor.OrderCommand> orderProcessor) {
        super(context);
        this.maxWeight = maxWeight;
        this.maxCapacity = maxCapacity;
        this.orderProcessor = orderProcessor;
        this.inventory = Collections.synchronizedList(new ArrayList<>());
        this.orderHistory = Collections.synchronizedList(new ArrayList<>());

        // Initialize with some products
        inventory.add(new Product("Milk", 2, 1.0f, 1.5f));
        inventory.add(new Product("Eggs", 10, 0.5f, 3.0f));

        context.getLog().info("Fridge initialized with capacity: {} items, max weight: {}kg",
                maxCapacity, maxWeight);
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ConsumeProduct.class, this::handleConsume)
                .onMessage(PlaceOrder.class, this::handlePlaceOrder)
                .onMessage(GetInventory.class, this::handleGetInventory)
                .onMessage(OrderCompleted.class, this::handleOrderCompleted)
                .onMessage(IgnoreResponse.class, this::handleIgnoreResponse)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> handleConsume(ConsumeProduct cmd) {
        Optional<Product> product = findProduct(cmd.productName);

        if (!product.isPresent()) {
            cmd.replyTo.tell(new OrderFailed("Product not found"));
            return this;
        }

        Product p = product.get();
        synchronized (p) {
            if (p.getQuantity() <= 0) {
                cmd.replyTo.tell(new OrderFailed("Product out of stock"));
                return this;
            }

            p.setQuantity(p.getQuantity() - 1);
            getContext().getLog().info("Consumed 1 {}. Remaining: {}", p.getName(), p.getQuantity());
            cmd.replyTo.tell(new OrderSuccess("Consumed successfully"));

            if (p.getQuantity() <= 2) { // Reorder threshold
                ActorRef<OrderResponse> adapter = getContext().messageAdapter(
                        OrderResponse.class,
                        IgnoreResponse::new
                );
                Order newOrder = new Order(p.getName(), 5, "Pending");
                getContext().getSelf().tell(new PlaceOrder(newOrder, adapter));
                getContext().getLog().info("Triggered auto-reorder for {}", p.getName());
            }
        }
        return this;
    }

    private Behavior<FridgeCommand> handlePlaceOrder(PlaceOrder cmd) {
        Order order = cmd.order;

        synchronized (inventory) {
            if (calculateTotalItems() + order.getQuantity() > maxCapacity) {
                cmd.replyTo.tell(new OrderFailed("Exceeds fridge capacity"));
                return this;
            }

            if (calculateTotalWeight() + (order.getQuantity() * 0.5f) > maxWeight) {
                cmd.replyTo.tell(new OrderFailed("Exceeds weight limit"));
                return this;
            }

            orderProcessor.tell(new OrderProcessor.ProcessOrder(
                    order.getProductName(),
                    order.getQuantity(),
                    getContext().messageAdapter(
                            OrderProcessor.OrderResponse.class,
                            response -> new OrderCompleted(response, order)
                    )
            ));

            orderHistory.add(new Order(order.getProductName(), order.getQuantity(), "Processing"));
            getContext().getLog().info("Order placed: {} x {}", order.getQuantity(), order.getProductName());
        }
        return this;
    }

    private Behavior<FridgeCommand> handleOrderCompleted(OrderCompleted cmd) {
        Order order = cmd.order;

        if (cmd.response instanceof OrderProcessor.OrderSuccess) {
            OrderProcessor.OrderSuccess success = (OrderProcessor.OrderSuccess) cmd.response;
            updateInventory(order.getProductName(), order.getQuantity());
            updateOrderHistory(order, "Completed");
            getContext().getLog().info("Order completed: {}", success.details);
        } else if (cmd.response instanceof OrderProcessor.OrderFailed) {
            OrderProcessor.OrderFailed failed = (OrderProcessor.OrderFailed) cmd.response;
            updateOrderHistory(order, "Failed: " + failed.reason);
            getContext().getLog().warn("Order failed: {}", failed.reason);
        }
        return this;
    }

    private Behavior<FridgeCommand> handleIgnoreResponse(IgnoreResponse cmd) {
        if (cmd.getResponse() instanceof OrderSuccess) {
            getContext().getLog().debug("Auto-reorder succeeded: {}",
                    ((OrderSuccess)cmd.getResponse()).message);
        } else {
            getContext().getLog().warn("Auto-reorder failed: {}",
                    ((OrderFailed)cmd.getResponse()).reason);
        }
        return this;
    }

    private Behavior<FridgeCommand> handleGetInventory(GetInventory cmd) {
        cmd.replyTo.tell(new InventoryResponse(
                new ArrayList<>(inventory),
                new ArrayList<>(orderHistory)
        ));
        return this;
    }

    private Optional<Product> findProduct(String name) {
        synchronized (inventory) {
            return inventory.stream()
                    .filter(p -> p.getName().equals(name))
                    .findFirst();
        }
    }

    private void updateInventory(String productName, int quantity) {
        synchronized (inventory) {
            findProduct(productName).ifPresentOrElse(
                    p -> p.setQuantity(p.getQuantity() + quantity),
                    () -> inventory.add(new Product(productName, quantity, 0.5f, 1.0f))
            );
        }
    }

    private void updateOrderHistory(Order order, String status) {
        synchronized (orderHistory) {
            orderHistory.removeIf(o ->
                    o.getProductName().equals(order.getProductName()) &&
                            o.getQuantity() == order.getQuantity() &&
                            o.getStatus().equals("Processing")
            );
            orderHistory.add(new Order(order.getProductName(), order.getQuantity(), status));
        }
    }

    private float calculateTotalWeight() {
        synchronized (inventory) {
            return (float) inventory.stream()
                    .mapToDouble(p -> p.getWeight() * p.getQuantity())
                    .sum();
        }
    }

    private int calculateTotalItems() {
        synchronized (inventory) {
            return inventory.stream()
                    .mapToInt(Product::getQuantity)
                    .sum();
        }
    }

    private Behavior<FridgeCommand> onPostStop() {
        getContext().getLog().info("Fridge stopped");
        return this;
    }
}
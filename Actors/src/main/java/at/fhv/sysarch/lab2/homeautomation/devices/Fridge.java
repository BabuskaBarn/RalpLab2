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

        public PlaceOrder(Order order , ActorRef<OrderResponse> replyTo) {
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
        public final String productName;
        public final int quantity;

        public OrderCompleted(OrderProcessor.OrderResponse response,
                              String productName, int quantity) {
            this.response = response;
            this.productName = productName;
            this.quantity = quantity;
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
        this.inventory = new ArrayList<>();
        this.orderHistory = new ArrayList<>();

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

        if (product.isPresent()) {
            Product p = product.get();
            p.setQuantity(p.getQuantity() - 1);

            getContext().getLog().info("Consumed 1 {}. Remaining: {}", p.getName(), p.getQuantity());
            cmd.replyTo.tell(new OrderSuccess("Consumed successfully"));

            if (p.getQuantity() <= 0) {
                ActorRef<OrderResponse> adapter = getContext().messageAdapter(
                        OrderResponse.class,
                        IgnoreResponse::new
                );

                getContext().getSelf().tell(
                        new PlaceOrder(p.getName(), 5, adapter)
                );
                getContext().getLog().info("Triggered auto-reorder for {}", p.getName());
            }
        } else {
            cmd.replyTo.tell(new OrderFailed("Product not found"));
        }
        return this;
    }

    private Behavior<FridgeCommand> handlePlaceOrder(PlaceOrder cmd) {
        if (calculateTotalItems() + cmd.quantity > maxCapacity) {
            if (cmd.replyTo != null) {
                cmd.replyTo.tell(new OrderFailed("Exceeds fridge capacity"));
            }
            return this;
        }

        if (calculateTotalWeight() + (cmd.quantity * 0.5f) > maxWeight) {
            if (cmd.replyTo != null) {
                cmd.replyTo.tell(new OrderFailed("Exceeds weight limit"));
            }
            return this;
        }

        orderProcessor.tell(new OrderProcessor.ProcessOrder(
                cmd.productName,
                cmd.quantity,
                getContext().messageAdapter(
                        OrderProcessor.OrderResponse.class,
                        response -> new OrderCompleted(response, cmd.productName, cmd.quantity)
                )
        ));

        orderHistory.add(new Order(cmd.productName, cmd.quantity, "Processing"));
        return this;
    }

    private Behavior<FridgeCommand> handleOrderCompleted(OrderCompleted cmd) {
        if (cmd.response instanceof OrderProcessor.OrderSuccess) {
            OrderProcessor.OrderSuccess success = (OrderProcessor.OrderSuccess) cmd.response;
            updateInventory(cmd.productName, cmd.quantity);
            updateOrderHistory(cmd.productName, cmd.quantity, "Completed");
            getContext().getLog().info("Order completed: {}", success.details);
        } else if (cmd.response instanceof OrderProcessor.OrderFailed) {
            OrderProcessor.OrderFailed failed = (OrderProcessor.OrderFailed) cmd.response;
            updateOrderHistory(cmd.productName, cmd.quantity, "Failed: " + failed.reason);
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
        cmd.replyTo.tell(new InventoryResponse(inventory, orderHistory));
        return this;
    }

    private Optional<Product> findProduct(String name) {
        return inventory.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();
    }

    private void updateInventory(String productName, int quantity) {
        findProduct(productName).ifPresentOrElse(
                p -> p.setQuantity(p.getQuantity() + quantity),
                () -> inventory.add(new Product(productName, quantity, 0.5f, 1.0f))
        );
    }

    private void updateOrderHistory(String productName, int quantity, String status) {
        orderHistory.removeIf(o ->
                o.getProductName().equals(productName) &&
                        o.getQuantity() == quantity &&
                        o.getStatus().equals("Processing")
        );
        orderHistory.add(new Order(productName, quantity, status));
    }

    private float calculateTotalWeight() {
        return (float) inventory.stream()
                .mapToDouble(p -> p.getWeight() * p.getQuantity())
                .sum();
    }

    private int calculateTotalItems() {
        return inventory.stream()
                .mapToInt(Product::getQuantity)
                .sum();
    }

    private Behavior<FridgeCommand> onPostStop() {
        getContext().getLog().info("Fridge stopped");
        return this;
    }
}
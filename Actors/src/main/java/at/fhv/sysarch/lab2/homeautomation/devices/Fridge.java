package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {

    public interface FridgeCommand {}

    // Command definitions
    public static final class ConsumeProduct implements FridgeCommand {
        final String productName;
        final ActorRef<OrderResponse> replyTo;

        public ConsumeProduct(String productName, ActorRef<OrderResponse> replyTo) {
            this.productName = productName;
            this.replyTo = replyTo;
        }
    }

    public static final class OrderProducts implements FridgeCommand {
        final String productName;
        final int quantity;
        final ActorRef<OrderResponse> replyTo;

        public OrderProducts(String productName, int quantity, ActorRef<OrderResponse> replyTo) {
            this.productName = productName;
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }

    public static final class GetProducts implements FridgeCommand {
        final ActorRef<ProductsResponse> replyTo;

        public GetProducts(ActorRef<ProductsResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class GetOrderHistory implements FridgeCommand {
        final ActorRef<OrderHistoryResponse> replyTo;

        public GetOrderHistory(ActorRef<OrderHistoryResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    // Response messages
    public interface OrderResponse {}
    public static final class OrderAccepted implements OrderResponse {
        final String message;

        public OrderAccepted(String message) {
            this.message = message;
        }
    }
    public static final class OrderRejected implements OrderResponse {
        final String reason;

        public OrderRejected(String reason) {
            this.reason = reason;
        }
    }
    public static final class ProductsResponse {
        final List<Product> products;

        public ProductsResponse(List<Product> products) {
            this.products = new ArrayList<>(products);
        }
    }
    public static final class OrderHistoryResponse {
        final List<Order> orders;

        public OrderHistoryResponse(List<Order> orders) {
            this.orders = new ArrayList<>(orders);
        }
    }

    // Product and Order data classes
    public static class Product {
        private final String name;
        private int quantity;
        private final float weight;
        private final float price;

        public Product(String name, int quantity, float weight, float price) {
            this.name = name;
            this.quantity = quantity;
            this.weight = weight;
            this.price = price;
        }

        // Getters and setters...
    }

    public static class Order {
        private final String productName;
        private final int quantity;
        private final String status;

        public Order(String productName, int quantity, String status) {
            this.productName = productName;
            this.quantity = quantity;
            this.status = status;
        }
    }

    private final List<Product> products;
    private final List<Order> orderHistory;
    private final float maxWeight;
    private final int maxCapacity;
    private final ActorRef<OrderProcessor.OrderCommand> orderProcessor;

    public static Behavior<FridgeCommand> create(
            float maxWeight,
            int maxCapacity,
            ActorRef<OrderProcessor.OrderCommand> orderProcessor) {
        return Behaviors.setup(context -> new Fridge(context, maxWeight, maxCapacity, orderProcessor));
    }

    private Fridge(
            ActorContext<FridgeCommand> context,
            float maxWeight,
            int maxCapacity,
            ActorRef<OrderProcessor.OrderCommand> orderProcessor) {
        super(context);
        this.maxWeight = maxWeight;
        this.maxCapacity = maxCapacity;
        this.orderProcessor = orderProcessor;
        this.products = new ArrayList<>();
        this.orderHistory = new ArrayList<>();

        // Initial products
        products.add(new Product("Milk", 2, 1.0f, 1.5f));
        products.add(new Product("Eggs", 10, 0.5f, 3.0f));

        getContext().getLog().info("Fridge started with capacity {} items and max weight {}", maxCapacity, maxWeight);
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ConsumeProduct.class, this::onConsumeProduct)
                .onMessage(OrderProducts.class, this::onOrderProducts)
                .onMessage(GetProducts.class, this::onGetProducts)
                .onMessage(GetOrderHistory.class, this::onGetOrderHistory)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onConsumeProduct(ConsumeProduct cmd) {
        Optional<Product> productOpt = products.stream()
                .filter(p -> p.getName().equals(cmd.productName))
                .findFirst();

        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setQuantity(product.getQuantity() - 1);

            getContext().getLog().info("Consumed 1 {}. Remaining: {}", product.getName(), product.getQuantity());
            cmd.replyTo.tell(new OrderAccepted("Product consumed"));

            if (product.getQuantity() <= 0) {
                // Auto-reorder
                getContext().getSelf().tell(
                        new OrderProducts(product.getName(), 5, ActorRef.noSender()),
                        getContext().getSelf()
                );
            }
        } else {
            cmd.replyTo.tell(new OrderRejected("Product not found"));
        }

        return this;
    }

    private Behavior<FridgeCommand> onOrderProducts(OrderProducts cmd) {
        float currentWeight = calculateTotalWeight();
        int currentItems = products.size();

        // Check capacity
        if (currentItems + cmd.quantity > maxCapacity) {
            cmd.replyTo.tell(new OrderRejected("Not enough space in fridge"));
            return this;
        }

        // Check weight (simplified - assuming each product has same weight)
        if (currentWeight + (cmd.quantity * 0.5f) > maxWeight) {
            cmd.replyTo.tell(new OrderRejected("Would exceed maximum weight"));
            return this;
        }

        // Forward to order processor
        orderProcessor.tell(new OrderProcessor.ProcessOrder(
                cmd.productName,
                cmd.quantity,
                getContext().getSelf()
        ));

        return this;
    }

    private Behavior<FridgeCommand> onGetProducts(GetProducts cmd) {
        cmd.replyTo.tell(new ProductsResponse(products));
        return this;
    }

    private Behavior<FridgeCommand> onGetOrderHistory(GetOrderHistory cmd) {
        cmd.replyTo.tell(new OrderHistoryResponse(orderHistory));
        return this;
    }

    private float calculateTotalWeight() {
        return products.stream()
                .map(p -> p.getWeight() * p.getQuantity())
                .reduce(0f, Float::sum);
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor stopped");
        return this;
    }
}
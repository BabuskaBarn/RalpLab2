package at.fhv.sysarch.lab2.homeautomation.devices.Fridge;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.grpc.GrpcClientSettings;
import at.fhv.sysarch.lab2.homeautomation.devices.Fridge.fridgeComponents.Product;
import at.fhv.sysarch.lab2.homeautomation.devices.Fridge.fridgeComponents.Order;
import at.fhv.sysarch.lab2.homeautomation.order.proto.OrderServiceClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {



    public interface FridgeCommand{
    }

    private final List<Order> _orders;
    private final List<Product> _products;
    public final static double MAX_WEIGHT = 250;
    public final static int MAX_SPACE = 10;
    private final OrderServiceClient orderClient;

    private final ActorRef<WeightSensor.WeightSensorCommand> _weightSensor;
    private final ActorRef<SpaceSensor.SpaceSensorCommand> _spaceSensor;

    public static final class AddProduct implements FridgeCommand {
        public final Product product;

        public AddProduct(Product product) {
            this.product = product;
        }
    }

    public static final class RemoveProduct implements FridgeCommand {
        public final Product product;

        public RemoveProduct(Product product) {
            this.product = product;
        }
    }

    public static final class OrderProducts implements FridgeCommand {
        public final Order order;
        public OrderProducts(Order order) {
            this.order = order;
        }
    }

    public static final class AddOrder implements FridgeCommand {
        public final Order order;

        public AddOrder(Order order) {
            this.order = order;
        }
    }

    public static class StateRequestCommand implements FridgeCommand {
        private final ActorRef<StateResponse> sender;

        public StateRequestCommand(ActorRef<StateResponse> sender) {
            this.sender = sender;
        }

        public ActorRef<StateResponse> getSender() {
            return sender;
        }
    }

    public static class StateResponse implements FridgeCommand {
        private final List<Order> orders;

        private final List<Product> products;
        public StateResponse(List<Order> orders, List<Product> products) {

            this.orders = orders;
            this.products = products;
        }

        public List<Order> getOrders() {
            return orders;
        }

        public List<Product> getProducts() {
            return products;
        }
    }

    private double getCurrentProductWeight() {
        return _products.stream().mapToDouble(Product::getWeight).sum();
    }

    private int getCurrentProductSpace() {
        return _products.size();
    }

    public Fridge(ActorContext<FridgeCommand> context){
        super(context);
        this._orders = new ArrayList<>();
        this._products = new ArrayList<>();
        this._weightSensor = context.spawn(WeightSensor.create(MAX_WEIGHT), "WeightSensor");
        this._spaceSensor = context.spawn(SpaceSensor.create(MAX_SPACE), "SpaceSensor");

        this.orderClient = OrderServiceClient.create(
                GrpcClientSettings.connectToServiceAt("localhost", 50051, context.getSystem())
                        .withTls(false),
                context.getSystem()
        );
    }


    public static Behavior create() {
        return Behaviors.setup(Fridge::new);
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(AddProduct.class, this::onAddProduct)
                .onMessage(RemoveProduct.class, this::onRemoveProduct)
                .onMessage(OrderProducts.class, this::onOrderProducts)
                .onMessage(AddOrder.class, this::onAddOrder)
                .onMessage(StateRequestCommand.class, this::onStateRequest)
                .build();
    }

    private Behavior<FridgeCommand> onAddProduct(AddProduct command) {
        Product product = command.product;
        double newWeight = getCurrentProductWeight() + product.getWeight();
        int newSpace = getCurrentProductSpace() + 1;

        if (newWeight <= MAX_WEIGHT && newSpace <= MAX_SPACE) {
            _products.add(product);
            _weightSensor.tell(new WeightSensor.UpdateWeight(newWeight));
            _spaceSensor.tell(new SpaceSensor.UpdateSpace(newSpace));
            getContext().getLog().info("Added product: {}, Current weight: {}, Current space: {}", product.getName(), newWeight, newSpace);
        } else {
            getContext().getLog().info("Cannot add product: {}. Exceeds weight or volume limits.", product.getName());
        }
        return this;
    }

    private Behavior<FridgeCommand> onRemoveProduct(RemoveProduct command) {
        Product productToRemove = command.product;
        List<Product> matchingProducts = _products.stream().filter(p -> p.equals(productToRemove)).toList();

        if (!matchingProducts.isEmpty()) {
            _products.remove(productToRemove);
            getContext().getLog().info("Removed product: {}", productToRemove.getName());

            double newWeight = getCurrentProductWeight();
            int newSpace = getCurrentProductSpace();
            _weightSensor.tell(new WeightSensor.UpdateWeight(newWeight));
            _spaceSensor.tell(new SpaceSensor.UpdateSpace(newSpace));

            if (matchingProducts.size() == 1) {
                getContext().getLog().info("Last {} consumed, reordering: ", productToRemove.getName());
                Order newOrder = new Order(List.of(new Product(productToRemove.getName(), productToRemove.getPrice(), productToRemove.getWeight())));
                getContext().getSelf().tell(new OrderProducts(newOrder));
            }
        } else {
            getContext().getLog().info("Product not found: {}", productToRemove.getName());
        }
        return this;
    }

    private Behavior<FridgeCommand> onOrderProducts(OrderProducts command) {
        getContext().spawn(OrderProcessor.create(getContext().getSelf(), _weightSensor, _spaceSensor, command.order, orderClient), "Orderproccessor" + UUID.randomUUID());
        return this;
    }

    private Behavior<FridgeCommand> onAddOrder(AddOrder command) {
        _orders.add(command.order);
        if (command.order.isSuccess()) {
            getContext().getLog().info("Order successful: " + command.order.getReceipt());
        } else {
            getContext().getLog().info("Order failed: " + command.order.getId());
        }

        command.order.getProducts().forEach(product -> {
            getContext().getSelf().tell(new Fridge.AddProduct(product));
        });

        return this;
    }


    private Behavior<FridgeCommand> onStateRequest(StateRequestCommand stateRequest) {
        stateRequest.getSender().tell(new StateResponse(_orders, _products));
        return this;
    }

    public static class SimpleHistoryResponse implements FridgeCommand {
        public final String summary;

        public SimpleHistoryResponse(List<Order> orders) {
            this.summary = orders.stream()
                    .map(o -> o.getId() + ": " + o.getProducts().size() + " items")
                    .collect(Collectors.joining("\n"));
        }
    }
}

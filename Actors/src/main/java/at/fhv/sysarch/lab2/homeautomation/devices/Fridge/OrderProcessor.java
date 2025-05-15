package at.fhv.sysarch.lab2.homeautomation.devices.Fridge;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.Fridge.fridgeComponents.Order;
import at.fhv.sysarch.lab2.homeautomation.order.proto.OrderRequest;
import at.fhv.sysarch.lab2.homeautomation.order.proto.OrderServiceClient;
import at.fhv.sysarch.lab2.homeautomation.order.proto.Product;
import at.fhv.sysarch.lab2.homeautomation.order.proto.OrderResponse;


import java.util.List;
import java.util.Optional;

public class OrderProcessor extends AbstractBehavior<OrderProcessor.OrderProcessorCommand> {

    public interface OrderProcessorCommand{
    }

    private final ActorRef<Fridge.FridgeCommand> _fridge;
    private final ActorRef<WeightSensor.WeightSensorCommand> _weightSensor;
    private final ActorRef<SpaceSensor.SpaceSensorCommand> _spaceSensor;
    private Order _order;
    private Optional<Double> _currentAvailableWeight;
    private Optional<Integer> _currentAvailableSpace;

    private final OrderServiceClient _orderServiceClient;

    public static final class ReceiveWeight implements OrderProcessorCommand {
        double weight;

        public ReceiveWeight(double weight) {
            this.weight = weight;
        }
    }

    public static final class ReceiveSpace implements OrderProcessorCommand {
        int space;

        public ReceiveSpace(int space) {
            this.space = space;
        }
    }


    public OrderProcessor(ActorContext<OrderProcessorCommand> context, ActorRef<Fridge.FridgeCommand> fridge, ActorRef<WeightSensor.WeightSensorCommand> weightSensor, ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor, Order order,  OrderServiceClient orderServiceClient) {
        super(context);
        _fridge = fridge;
        _weightSensor = weightSensor;
        _spaceSensor = spaceSensor;
        _order = order;
        _orderServiceClient = orderServiceClient;

        _weightSensor.tell(new WeightSensor.QueryWeight(getContext().getSelf()));
        _spaceSensor.tell(new SpaceSensor.QuerySpace(getContext().getSelf()));

        getContext().getLog().info("Start order");
    }

    public Behavior<OrderProcessorCommand> onReceiveWeight(ReceiveWeight command){
        _currentAvailableWeight = Optional.of(command.weight);

        getContext().getLog().info("OrderProcessor received new Weight: {}", _currentAvailableWeight);
        return continueOrComplete();
    }

    public Behavior<OrderProcessorCommand> onReceiveSpace(ReceiveSpace command){
        _currentAvailableSpace = Optional.of(command.space);

        getContext().getLog().info("OrderProcessor received new Space: {}", _currentAvailableSpace);
        return continueOrComplete();
    }

    public static Behavior<OrderProcessorCommand> create( ActorRef<Fridge.FridgeCommand> fridge, ActorRef<WeightSensor.WeightSensorCommand> weightSensor, ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor, Order order, OrderServiceClient orderServiceClient){
        return Behaviors.setup(context -> new OrderProcessor(context, fridge, weightSensor, spaceSensor, order, orderServiceClient));
    }

    private Behavior<OrderProcessorCommand> continueOrComplete() {
        if (_currentAvailableSpace != null && _currentAvailableWeight != null) {
            if (_currentAvailableWeight.get() > _order.getOrderWeight() &&
                    _currentAvailableSpace.get() > 0) {

                // 1. Produkte konvertieren
                List<at.fhv.sysarch.lab2.homeautomation.order.proto.Product> grpcProducts =
                        _order.getProducts().stream()
                                .map(p -> at.fhv.sysarch.lab2.homeautomation.order.proto.Product.newBuilder()
                                        .setName(p.getName())
                                        .setPrice(p.getPrice())
                                        .setWeight(p.getWeight())
                                        .build())
                                .toList();

                // 2. Request bauen

                OrderRequest.Builder requestBuilder = (OrderRequest.Builder) OrderRequest.newBuilder()
                        .setId(_order.getId().toString());

                for (at.fhv.sysarch.lab2.homeautomation.order.proto.Product p : grpcProducts) {
                    requestBuilder.addProducts(p);
                }

                OrderRequest grpcOrder = requestBuilder.build();


                // 3. gRPC-Aufruf mit korrektem Methodennamen
                _orderServiceClient.processOrder(grpcOrder).thenAccept(response -> {
                    _order.setSuccess(response.getSuccess());
                    _order.setReceipt(response.getReceipt());

                    if (response.getSuccess()) {
                        _order.getProducts().forEach(p ->
                                _fridge.tell(new Fridge.AddProduct(p)));
                    }

                    _fridge.tell(new Fridge.AddOrder(_order));
                });

            } else {
                // ... Fehlerbehandlung
            }
            return this;
        }
        return this;
    }


    private String generateReceipt(Order order) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Order ID: ").append(order.getId()).append("\n");
        receipt.append("Items:\n");
        order.getProducts().forEach(product -> receipt.append(product.getName()).append(" - €").append(product.getPrice()).append("\n"));
        receipt.append("Total Price: €").append(order.getOrderPrice()).append("\n");
        return receipt.toString();
    }
    @Override
    public Receive<OrderProcessorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReceiveWeight.class, this::onReceiveWeight)
                .onMessage(ReceiveSpace.class, this::onReceiveSpace)
                .build();
    }
}

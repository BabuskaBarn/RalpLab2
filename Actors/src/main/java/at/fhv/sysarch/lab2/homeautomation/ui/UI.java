package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.handler.InputHandler;
import at.fhv.sysarch.lab2.ordersystem.internal.OrderProcessor;

public class UI extends AbstractBehavior<UI.UserInput> {

    public interface UserInput {
        String getInput();
    }

    public static final class RawInput implements UserInput {
        private final String input;

        public RawInput(String input) {
            this.input = input;
        }

        @Override
        public String getInput() {
            return input;
        }
    }

    private static final class IgnoreResponse implements UserInput {
        @Override
        public String getInput() {
            return "";
        }
    }

    private final ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ActorRef<Fridge.FridgeCommand> fridge;
    private final ActorRef<OrderProcessor.OrderCommand> orderProcessor;
    private final ActorRef<InputHandler.Command> inputHandler;

    public static Behavior<UserInput> create(
            ActorRef<TemperatureSensor.TemperatureCommand> tempSensor,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<Fridge.FridgeCommand> fridge,
            ActorRef<OrderProcessor.OrderCommand> orderProcessor) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, fridge, orderProcessor));
    }

    private UI(ActorContext<UserInput> context,
               ActorRef<TemperatureSensor.TemperatureCommand> tempSensor,
               ActorRef<AirCondition.AirConditionCommand> airCondition,
               ActorRef<Fridge.FridgeCommand> fridge,
               ActorRef<OrderProcessor.OrderCommand> orderProcessor) {
        super(context);
        this.tempSensor = tempSensor;
        this.airCondition = airCondition;
        this.fridge = fridge;
        this.orderProcessor = orderProcessor;

        this.inputHandler = context.spawn(InputHandler.create(), "inputHandler");
        this.inputHandler.tell(new InputHandler.StartInputLoop(getContext().getSelf()));

        printWelcomeMessage();
        context.getLog().info("UI started");
    }

    @Override
    public Receive<UserInput> createReceive() {
        return newReceiveBuilder()
                .onMessage(RawInput.class, this::handleInput)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<UserInput> handleInput(RawInput msg) {
        String[] parts = msg.getInput().split("\\s+");
        if (parts.length == 0) return this;

        String command = parts[0].toLowerCase();
        try {
            switch (command) {
                case "t":
                case "temp":
                    handleTemperatureCommand(parts);
                    break;
                case "ac":
                    handleAirConditionCommand(parts);
                    break;
                case "order":
                    handleOrderCommand(parts);
                    break;
                case "inventory":
                    handleInventoryCommand();
                    break;
                case "consume":
                    handleConsumeCommand(parts);
                    break;
                case "help":
                    printHelp();
                    break;
                case "quit":
                    return Behaviors.stopped();
                default:
                    System.out.println("Unknown command. Type 'help' for available commands.");
            }
        } catch (Exception e) {
            getContext().getLog().error("Error processing command: {}", e.getMessage());
            System.out.println("Error: " + e.getMessage());
        }
        return this;
    }

    private void handleTemperatureCommand(String[] parts) {
        if (parts.length >= 2) {
            double temperature = Double.parseDouble(parts[1]);
            tempSensor.tell(new TemperatureSensor.ReadTemperature(temperature));
            System.out.println("Temperature set to: " + temperature + "°C");
        } else {
            System.out.println("Usage: temp <temperature>");
        }
    }

    private void handleAirConditionCommand(String[] parts) {
        if (parts.length >= 2) {
            boolean active = Boolean.parseBoolean(parts[1]);
            airCondition.tell(new AirCondition.PowerAirCondition(active));
            System.out.println("Air Condition " + (active ? "activated" : "deactivated"));
        } else {
            System.out.println("Usage: ac <true|false>");
        }
    }

    private void handleOrderCommand(String[] parts) {
        if (parts.length >= 3) {
            String product = parts[1];
            int quantity = Integer.parseInt(parts[2]);

            ActorRef<Fridge.OrderResponse> replyTo = getContext().messageAdapter(
                    Fridge.OrderResponse.class,
                    response -> {
                        if (response instanceof Fridge.OrderSuccess) {
                            System.out.println("Order success: " + ((Fridge.OrderSuccess) response).message);
                        } else {
                            System.out.println("Order failed: " + ((Fridge.OrderFailed) response).reason);
                        }
                        return new IgnoreResponse();
                    }
            );

            fridge.tell(new Fridge.PlaceOrder(product, quantity, replyTo));
            System.out.println("Processing order for " + quantity + " x " + product);
        } else {
            System.out.println("Usage: order <product> <quantity>");
        }
    }

    private void handleInventoryCommand() {
        ActorRef<Fridge.InventoryResponse> replyTo = getContext().messageAdapter(
                Fridge.InventoryResponse.class,
                response -> {
                    System.out.println("\n=== FRIDGE INVENTORY ===");
                    System.out.println("Products:");
                    response.products.forEach(p ->
                            System.out.printf("- %s: %d units (%.1fkg each)\n",
                                    p.getName(), p.getQuantity(), p.getWeight()));

                    System.out.println("\nOrder History:");
                    response.orderHistory.forEach(o ->
                            System.out.printf("- %d x %s: %s\n",
                                    o.getQuantity(), o.getProductName(), o.getStatus()));
                    return new IgnoreResponse();
                }
        );
        fridge.tell(new Fridge.GetInventory(replyTo));
    }

    private void handleConsumeCommand(String[] parts) {
        if (parts.length >= 2) {
            String product = parts[1];

            ActorRef<Fridge.OrderResponse> replyTo = getContext().messageAdapter(
                    Fridge.OrderResponse.class,
                    response -> {
                        if (response instanceof Fridge.OrderSuccess) {
                            System.out.println("Consumed: " + product);
                        } else {
                            System.out.println("Failed to consume: " + ((Fridge.OrderFailed) response).reason);
                        }
                        return new IgnoreResponse();
                    }
            );

            fridge.tell(new Fridge.ConsumeProduct(product, replyTo));
        } else {
            System.out.println("Usage: consume <product>");
        }
    }

    private void printWelcomeMessage() {
        System.out.println("\n=== SMART HOME CONTROL SYSTEM ===");
        System.out.println("Type 'help' for available commands");
    }

    private void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  temp <value>       - Set temperature (e.g., 'temp 22.5')");
        System.out.println("  ac <true|false>    - Toggle air conditioner");
        System.out.println("  order <prod> <qty> - Place fridge order (e.g., 'order Milk 2')");
        System.out.println("  consume <product>  - Consume product from fridge");
        System.out.println("  inventory          - Show fridge contents and history");
        System.out.println("  help               - Show this help");
        System.out.println("  quit               - Exit the system\n");
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        System.out.println("System shutting down...");
        return this;
    }
}
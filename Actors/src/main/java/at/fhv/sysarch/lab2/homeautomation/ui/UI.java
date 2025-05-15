package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.devices.Fridge.Fridge;
import at.fhv.sysarch.lab2.homeautomation.devices.MediaStation.MediaStationCommand;
import at.fhv.sysarch.lab2.homeautomation.devices.Messages.Environment;
import at.fhv.sysarch.lab2.homeautomation.devices.Fridge.fridgeComponents.Order;
import at.fhv.sysarch.lab2.homeautomation.devices.states.MovieState;
import at.fhv.sysarch.lab2.homeautomation.devices.states.WeatherState;
import at.fhv.sysarch.lab2.homeautomation.order.proto.OrderRequest;
import at.fhv.sysarch.lab2.homeautomation.devices.Fridge.fridgeComponents.Product;
import at.fhv.sysarch.lab2.homeautomation.devices.Fridge.fridgeComponents.Order;

import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<MediaStationCommand> mediaStation;
    private ActorRef<Blinds.BlindsCommand> blinds;
    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<WeatherSensor.WeatherCommand> weather;
    private ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<Void> create(ActorRef<MediaStationCommand> mediaStation,
                                        ActorRef<AirCondition.AirConditionCommand> airCondition,
                                        ActorRef<Blinds.BlindsCommand> blinds,
                                        ActorRef<TemperatureSensor.TemperatureCommand> tempSensor,
                                        ActorRef<WeatherSensor.WeatherCommand> weather,
                                        ActorRef<Fridge.FridgeCommand> fridge) {
        return Behaviors.setup(context -> new UI(context, mediaStation, airCondition, environment, blinds, tempSensor, weather, fridge));
    }

    private UI(ActorContext<Void> context,
               ActorRef<MediaStationCommand> mediaStation,
               ActorRef<AirCondition.AirConditionCommand> airCondition,
               ActorRef<Blinds.BlindsCommand> blinds,
               ActorRef<TemperatureSensor.TemperatureCommand> tempSensor,
               ActorRef<WeatherSensor.WeatherCommand> weather,
               ActorRef<Fridge.FridgeCommand> fridge) {
        super(context);
        this.fridge = fridge;
        this.mediaStation = mediaStation;
        this.blinds = blinds;
        this.tempSensor = tempSensor;
        this.weather = weather;
        this.airCondition = airCondition;
        getContext().getLog().info("UI started");

        new Thread(this::runCommandLine).start();
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        Scanner scanner = new Scanner(System.in);
        String reader;
        System.out.println("Type 'exit' to quit.");
        System.out.println("Available commands: media, blinds, aircondition, fridge");
        while (scanner.hasNextLine()) {
            reader = scanner.nextLine();
            if (reader.equalsIgnoreCase("exit")) {
                System.out.println("Bye");
                System.exit(0);
                break;
            }
            handleCommand(reader);
        }
    }

    private void handleCommand(String command) {
        String[] parts = command.split(" ");
        switch (parts[0].toLowerCase()) {
            case "media":
                handleMedia(parts);
                break;
            case "blinds":
                handleBlinds(parts);
                break;
            case "aircondition":
                handleAirCondition(parts);
                break;
            case "fridge":
                handleFridge(parts);
                break;
            case "source":
                handleSource(parts);
                break;
            default:
                System.out.println("Unknown command.");
                break;
        }
    }


    private void handleMedia(String[] parts) {
        if (parts.length > 1) {
            switch (parts[1].toLowerCase()) {
                case "play":
                    if (parts.length < 3) {
                        System.out.println("No movie specified!");
                        break;
                    }
                    String movieName = parts[2];
                    MovieState movieToPlay = getMovieByName(movieName);
                    if (movieToPlay != null) {
                        mediaStation.tell(new MediaStation.PlayMovie(movieToPlay));
                    } else {
                        System.out.println("Movie not found");
                    }
                    break;
                case "stop":
                    mediaStation.tell(new MediaStation.StopMovie());
                    System.out.println("Tried to stop the movie");
                    break;
                case "state":
                    mediaStation.tell(new MediaStation.GetStatus());
                    System.out.println("Tried to get the status of the media station");
                    break;
                default:
                    System.out.println("Invalid media command");
                    break;
            }
        }
    }

    private void handleBlinds(String[] parts) {
        blinds.tell(new Blinds.ToggleCommand());
    }

    private void handleAirCondition(String[] parts) {
        if (parts.length == 2) {
            switch (parts[1].toLowerCase()) {
                case "on":
                    airCondition.tell(new AirCondition.PowerAirCondition(true));
                    System.out.println("Manually turned ON the air conditioner.");
                    break;
                case "off":
                    airCondition.tell(new AirCondition.PowerAirCondition(false));
                    System.out.println("Manually turned OFF the air conditioner.");
                    break;
                default:
                    System.out.println("Unknown aircondition command. Use 'aircondition on' or 'aircondition off'.");
                    break;
            }
        } else {
            System.out.println("Usage: aircondition <on|off>");
        }
    }

    private void handleFridge(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Available commands: order <name> <price> <weight>, consume <name>, history");
            return;
        }

        switch (parts[1].toLowerCase()) {
            case "order":
                if (parts.length < 5) {
                    System.out.println("Usage: fridge order <name> <price> <weight>");
                    return;
                }
                try {
                    Product product = new Product(
                            parts[2], // name
                            Double.parseDouble(parts[3]), // price
                            Double.parseDouble(parts[4]) // weight
                    );
                    fridge.tell(new Fridge.OrderProducts(new Order(List.of(product))));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price/weight format");
                }
                break;

            case "consume":
                if (parts.length < 3) {
                    System.out.println("Usage: fridge consume <name>");
                    return;
                }
                fridge.tell(new Fridge.RemoveProduct(new Product(parts[2], 0.0, 0.0)));
                break;

            case "history":
                fridge.tell(new Fridge.SimpleHistoryResponse(null));
                break;

            default:
                System.out.println("Unknown fridge command");
                break;
        }


    }
    private void handleSource(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Usage: source <weather|temp> <mqtt|sim>");
            return;
        }

        boolean useExternal;
        switch (parts[2].toLowerCase()) {
            case "mqtt":
                useExternal = true;
                break;
            case "sim":
                useExternal = false;
                break;
            default:
                System.out.println("Invalid source type. Use 'mqtt' or 'sim'.");
                return;
        }

        switch (parts[1].toLowerCase()) {
            case "weather":
                weather.tell(new WeatherSensor.ToggleSource(useExternal));
                System.out.println("Weather source set to " + (useExternal ? "MQTT" : "Simulation"));
                break;
            case "temp":
                tempSensor.tell(new TemperatureSensor.ToggleSource(useExternal));
                System.out.println("Temperature source set to " + (useExternal ? "MQTT" : "Simulation"));
                break;
            default:
                System.out.println("Unknown sensor. Use 'weather' or 'temp'.");
                break;
        }
    }
    /*Umschalt Commands
source weather sim
source temp sim

source weather mqtt
source temp mqtt



     */
    private MovieState getMovieByName(String name) {
        for (MovieState movie : MovieState.values()) {
            if (movie.getName().equalsIgnoreCase(name)) {
                return movie;
            }
        }
        return null;
    }
}

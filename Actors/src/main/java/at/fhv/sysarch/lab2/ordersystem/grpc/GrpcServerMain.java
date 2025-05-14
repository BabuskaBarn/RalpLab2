package at.fhv.sysarch.lab2.ordersystem.grpc;

import at.fhv.sysarch.lab2.ordersystem.internal.OrderProcessor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class GrpcServerMain {

    public static void main(String[] args) throws Exception {
        ActorSystem<OrderProcessor.Command> system =
                ActorSystem.create(OrderProcessor.create(), "OrderSystem");

        Server server = ServerBuilder.forPort(50051)
                .addService((BindableService) new GrpcOrderService(system))
                .build();

        server.start();
        System.out.println("gRPC Server läuft auf Port 50051");
        server.awaitTermination();
    }
}

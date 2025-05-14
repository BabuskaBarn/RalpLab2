package at.fhv.sysarch.lab2.ordersystem.grpc;

import at.fhv.sysarch.lab2.homeautomation.order.proto.OrderRequest;
import at.fhv.sysarch.lab2.homeautomation.order.proto.OrderResponse;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.AskPattern;
import at.fhv.sysarch.lab2.ordersystem.internal.OrderProcessor;

import at.fhv.sysarch.lab2.ordersystem.internal.OrderServiceImpl;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static at.fhv.sysarch.lab2.homeautomation.order.proto.OrderResponse.*;

public class GrpcOrderService extends OrderServiceImpl {

    private final ActorRef<OrderProcessor.Command> orderProcessor;

    public GrpcOrderService(ActorRef<OrderProcessor.Command> orderProcessor) {
        this.orderProcessor = orderProcessor;
    }

    @Override
    public void placeOrder(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        String product = String.valueOf(request.getProduct());
        int quantity = request.getQuantity();

        CompletionStage<OrderProcessor.OrderResponse> result =
                AskPattern.ask(
                        orderProcessor,
                        replyTo -> new OrderProcessor.ProcessOrder(product, quantity, replyTo),
                        Duration.ofSeconds(3),
                        orderProcessor.getClass()
                );

        result.whenComplete((reply, error) -> {
            if (reply instanceof OrderProcessor.OrderSuccess) {
                OrderProcessor.OrderSuccess success = (OrderProcessor.OrderSuccess) reply;
                OrderResponse response = newBuilder()
                        .setSuccess(true)
                        .setReceipt(success.details)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else if (reply instanceof OrderProcessor.OrderFailed) {
                OrderProcessor.OrderFailed failed = (OrderProcessor.OrderFailed) reply;
                responseObserver.onError(new RuntimeException(failed.reason));
            } else {
                responseObserver.onError(error != null ? error : new RuntimeException("Unbekannter Fehler"));
            }
        });
    }
}

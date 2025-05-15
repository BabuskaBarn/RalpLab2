package at.fhv.sysarch.lab2.homeautomation.devices.Fridge.fridgeComponents;



import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Order {
    private final UUID _id;
    private final LocalDateTime _dateTime;
    private final List<Product> _products;
    private boolean _success;
    private String _receipt;

    public Order(List<Product> products) {
        _id = UUID.randomUUID();
        _dateTime = LocalDateTime.now();
        _products = products;
        _receipt = "";
        _success = false;
    }

    public Order(List<Product> products, boolean success, String receipt) {
        _id = UUID.randomUUID();
        _products = products;
        _success = success;
        _receipt = receipt;
        _dateTime = LocalDateTime.now();
    }

    public UUID getId() {
        return _id;
    }

    public LocalDateTime getDateTime() {
        return _dateTime;
    }

    public List<Product> getProducts() {
        return _products;
    }

    public int getOrderSpace() {
        return _products.size();
    }

    public double getOrderWeight() {
        return _products.stream().mapToDouble(Product::getWeight).sum();
    }

    public double getOrderPrice() {
        return _products.stream().mapToDouble(Product::getPrice).sum();
    }

    public boolean isSuccess() {
        return _success;
    }

    public void setSuccess(boolean success) {
        _success = success;
    }

    public String getReceipt() {
        return _receipt;
    }

    public void setReceipt(String receipt) {
        _receipt = receipt;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + _id +
                ", dateTime=" + _dateTime +
                ", products=" + _products +
                ", success=" + _success +
                ", receipt='" + _receipt + '\'' +
                '}';
    }
}

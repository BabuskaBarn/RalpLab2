package at.fhv.sysarch.lab2.homeautomation.devices.fridgeComponents;



public class Order {
    private final String productName;
    private final int quantity;
    private final String status;

    public Order(String productName, int quantity, String status) {
        this.productName = productName;
        this.quantity = quantity;
        this.status = status;
    }

    // Getters
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return String.format("%d x %s - Status: %s",
                quantity, productName, status);
    }
}
package at.fhv.sysarch.lab2.homeautomation.devices.fridgeComponents;




public class Product {
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

    // Getters and Setters
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public float getWeight() { return weight; }
    public float getPrice() { return price; }

    @Override
    public String toString() {
        return String.format("%s (Qty: %d, Weight: %.1fkg, Price: €%.2f)",
                name, quantity, weight, price);
    }
}

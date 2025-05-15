package at.fhv.sysarch.lab2.homeautomation.devices.Fridge.fridgeComponents;


import java.util.Objects;

public class Product {

    private final String _name;
    private final double _price;
    private final double _weight;

    public Product(String name, double price, double weight) {
        _name = name;
        _price = price;
        _weight = weight;
    }

    public String getName() {
        return _name;
    }

    public double getPrice() {
        return _price;
    }

    public double getWeight() {
        return _weight;
    }

    @Override
    public String toString() {
        return "Product{" +
                "name='" + _name + '\'' +
                ", price=" + _price +
                ", weight=" + _weight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return _name.equals(product.getName()) && _weight == product.getWeight() && Double.compare(product.getPrice(), _price) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_name, _price, _weight);
    }
}

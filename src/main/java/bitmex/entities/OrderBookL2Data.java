package bitmex.entities;

public class OrderBookL2Data {
    private String symbol;
    private long id;
    private String side;
    private long size;
    private float price;

    public OrderBookL2Data() {
    }

    public boolean equals(OrderBookL2Data other) {
        return this.id == other.id;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getId() {
        return id;
    }

    public String getSide() {
        return side;
    }

    public long getSize() {
        return size;
    }

    public float getPrice() {
        return price;
    }
}

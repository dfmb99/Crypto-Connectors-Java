package bitmex.entities;

public class OrderBookL2Data {
    private final String symbol;
    private final long id;
    private final String side;
    private final long size;
    private final float price;

    public OrderBookL2Data(String symbol, long id, String side, long size, float price) {
        this.symbol = symbol;
        this.id = id;
        this.side = side;
        this.size = size;
        this.price = price;
    }

    public boolean equals(OrderBookL2Data other) {
        return this.id == other.id;
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

package bitmex.ws.entities;

public class OrderBookL2Data {
    private String symbol;
    private Long id;
    private String side;
    private Long size;
    private Float price;

    public OrderBookL2Data() { }

    public boolean equals(OrderBookL2Data other) {
        return this.id.equals(other.id);
    }

    public String getSymbol() {
        return symbol;
    }

    public Long getId() {
        return id;
    }

    public String getSide() {
        return side;
    }

    public Long getSize() {
        return size;
    }

    public Float getPrice() {
        return price;
    }
}

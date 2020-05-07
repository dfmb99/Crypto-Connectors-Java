package bitmex.ws.entities;

public class LiquidationData {
    private String orderID;
    private String symbol;
    private String side;
    private Float price;
    private Long leavesQty;

    public LiquidationData() { }

    public String getOrderID() {
        return orderID;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public Float getPrice() {
        return price;
    }

    public Long getLeavesQty() {
        return leavesQty;
    }
}

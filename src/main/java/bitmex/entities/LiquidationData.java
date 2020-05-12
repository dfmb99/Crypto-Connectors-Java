package bitmex.entities;

public class LiquidationData {
    private final String orderID;
    private final String symbol;
    private final String side;
    private final Float price;
    private final Long leavesQty;

    public LiquidationData(String orderID, String symbol, String side, Float price, Long leavesQty) {
        this.orderID = orderID;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.leavesQty = leavesQty;
    }

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

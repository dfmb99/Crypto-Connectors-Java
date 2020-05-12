package bitmex.entities;

public class OrderData {
    private final String orderID;
    private final String clOrdID;
    private final String symbol;
    private final String side;
    private final Long orderQty;
    private final Float price;
    private final Float stopPx;
    private final String ordType;
    private final String timeInForce;
    private final String execInst;
    private final String ordStatus;
    private final Long leavesQty;
    private final Float cumQty;
    private final Float avgPx;
    private final String timestamp;

    public OrderData(String orderID, String clOrdID, String symbol, String side, Long orderQty, Float price, Float stopPx, String ordType, String timeInForce, String execInst, String ordStatus, Long leavesQty, Float cumQty, Float avgPx, String timestamp) {
        this.orderID = orderID;
        this.clOrdID = clOrdID;
        this.symbol = symbol;
        this.side = side;
        this.orderQty = orderQty;
        this.price = price;
        this.stopPx = stopPx;
        this.ordType = ordType;
        this.timeInForce = timeInForce;
        this.execInst = execInst;
        this.ordStatus = ordStatus;
        this.leavesQty = leavesQty;
        this.cumQty = cumQty;
        this.avgPx = avgPx;
        this.timestamp = timestamp;
    }

    public String getOrderID() {
        return orderID;
    }

    public String getClOrdID() {
        return clOrdID;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public Long getOrderQty() {
        return orderQty;
    }

    public Float getPrice() {
        return price;
    }

    public Float getStopPx() {
        return stopPx;
    }

    public String getOrdType() {
        return ordType;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public String getExecInst() {
        return execInst;
    }

    public String getOrdStatus() {
        return ordStatus;
    }

    public Long getLeavesQty() {
        return leavesQty;
    }

    public Float getCumQty() {
        return cumQty;
    }

    public Float getAvgPx() {
        return avgPx;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

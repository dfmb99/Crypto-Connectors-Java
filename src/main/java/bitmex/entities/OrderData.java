package bitmex.entities;

public class OrderData {
    private String orderID;
    private String clOrdID;
    private String symbol;
    private String side;
    private Long orderQty;
    private Float price;
    private Float stopPx;
    private String ordType;
    private String timeInForce;
    private String execInst;
    private String ordStatus;
    private Long leavesQty;
    private Float cumQty;
    private Float avgPx;
    private String timestamp;

    public OrderData() { }

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
        return this.timestamp;
    }
}

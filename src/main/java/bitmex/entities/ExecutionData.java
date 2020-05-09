package bitmex.entities;

public class ExecutionData {
    private String orderID;
    private String clOrdID;
    private String symbol;
    private String side;
    private Long orderQty;
    private Float price;
    private Float stopPx;
    private String ordType;
    private String execInst;
    private String timeInForce;
    private String ordStatus;
    private String leavesQty;
    private String cumQty;
    private String timestamp;

    public ExecutionData() { }

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

    public String getExecInst() {
        return execInst;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public String getOrdStatus() {
        return ordStatus;
    }

    public String getLeavesQty() {
        return leavesQty;
    }

    public String getCumQty() {
        return cumQty;
    }

    public String getTimestamp() {
        return this.timestamp;
    }
}

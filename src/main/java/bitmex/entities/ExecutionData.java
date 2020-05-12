package bitmex.entities;

public class ExecutionData {
    private final String orderID;
    private final String clOrdID;
    private final String symbol;
    private final String side;
    private final Long orderQty;
    private final Float price;
    private final Float stopPx;
    private final String ordType;
    private final String execInst;
    private final String timeInForce;
    private final String ordStatus;
    private final String leavesQty;
    private final String cumQty;
    private final String timestamp;

    public ExecutionData(String orderID, String clOrdID, String symbol, String side, Long orderQty, Float price, Float stopPx, String ordType, String execInst, String timeInForce, String ordStatus, String leavesQty, String cumQty, String timestamp) {
        this.orderID = orderID;
        this.clOrdID = clOrdID;
        this.symbol = symbol;
        this.side = side;
        this.orderQty = orderQty;
        this.price = price;
        this.stopPx = stopPx;
        this.ordType = ordType;
        this.execInst = execInst;
        this.timeInForce = timeInForce;
        this.ordStatus = ordStatus;
        this.leavesQty = leavesQty;
        this.cumQty = cumQty;
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
        return timestamp;
    }
}

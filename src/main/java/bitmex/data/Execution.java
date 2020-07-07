package bitmex.data;

public class Execution {
    private String execID, orderID, clOrdID, symbol, side, ordStatus, timestamp;
    private Float orderQty, price;

    public String getExecID() {
        return execID;
    }

    public void setExecID(String execID) {
        this.execID = execID;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getClOrdID() {
        return clOrdID;
    }

    public void setClOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getOrdStatus() {
        return ordStatus;
    }

    public void setOrdStatus(String ordStatus) {
        this.ordStatus = ordStatus;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Float getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(Float orderQty) {
        this.orderQty = orderQty;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }
}

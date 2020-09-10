package binance.data;

public class Order {
    private String clientOrderId, side, positionSide, status, symbol, timeInForce, type, origType, workingType;
    private Long orderId, updateTime;
    private Float cumQty, executedQty, origQty, cumQuote, avgPrice, price, stopPrice, activatePrice, priceRate;
    private Boolean reduceOnly, closePosition;

    public String getClientOrderId() {
        return clientOrderId;
    }

    public String getSide() {
        return side;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public String getStatus() {
        return status;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public String getType() {
        return type;
    }

    public String getOrigType() {
        return origType;
    }

    public String getWorkingType() {
        return workingType;
    }

    public Float getCumQty() {
        return cumQty;
    }

    public Float getCumQuote() {
        return cumQuote;
    }

    public Float getExecutedQty() {
        return executedQty;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Float getOrigQty() {
        return origQty;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public Float getAvgPrice() {
        return avgPrice;
    }

    public Float getPrice() {
        return price;
    }

    public Float getStopPrice() {
        return stopPrice;
    }

    public Float getActivatePrice() {
        return activatePrice;
    }

    public Float getPriceRate() {
        return priceRate;
    }

    public Boolean getReduceOnly() {
        return reduceOnly;
    }

    public Boolean getClosePosition() {
        return closePosition;
    }
}

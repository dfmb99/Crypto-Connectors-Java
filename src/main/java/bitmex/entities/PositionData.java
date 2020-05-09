package bitmex.entities;

public class PositionData {
    private String symbol;
    private Float initMarginReq;
    private Float maintMarginReq;
    private Float leverage;
    private Boolean crossMargin;
    private Long currentQty;
    private Float markPrice;
    private Long realisedPnl;
    private Long unrealisedPnl;
    private Float avgEntryPrice;
    private Float liquidationPrice;
    private Float bankruptPrice;
    private String timestamp;

    public PositionData() { }

    public String getSymbol() {
        return symbol;
    }

    public Float getInitMarginReq() {
        return initMarginReq;
    }

    public Float getMaintMarginReq() {
        return maintMarginReq;
    }

    public Float getLeverage() {
        return leverage;
    }

    public Boolean getCrossMargin() {
        return crossMargin;
    }

    public Long getCurrentQty() {
        return currentQty;
    }

    public Float getMarkPrice() {
        return markPrice;
    }

    public Long getRealisedPnl() {
        return realisedPnl;
    }

    public Long getUnrealisedPnl() {
        return unrealisedPnl;
    }

    public Float getAvgEntryPrice() {
        return avgEntryPrice;
    }

    public Float getLiquidationPrice() {
        return liquidationPrice;
    }

    public Float getBankruptPrice() {
        return bankruptPrice;
    }

    public String getTimestamp() {
        return this.timestamp;
    }
}

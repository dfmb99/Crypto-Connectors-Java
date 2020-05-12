package bitmex.entities;

public class PositionData {
    private final String symbol;
    private final Float initMarginReq;
    private final Float maintMarginReq;
    private final Float leverage;
    private final Boolean crossMargin;
    private final Long currentQty;
    private final Float markPrice;
    private final Long realisedPnl;
    private final Long unrealisedPnl;
    private final Float avgEntryPrice;
    private final Float liquidationPrice;
    private final Float bankruptPrice;
    private final String timestamp;

    public PositionData(String symbol, Float initMarginReq, Float maintMarginReq, Float leverage, Boolean crossMargin, Long currentQty, Float markPrice, Long realisedPnl, Long unrealisedPnl, Float avgEntryPrice, Float liquidationPrice, Float bankruptPrice, String timestamp) {
        this.symbol = symbol;
        this.initMarginReq = initMarginReq;
        this.maintMarginReq = maintMarginReq;
        this.leverage = leverage;
        this.crossMargin = crossMargin;
        this.currentQty = currentQty;
        this.markPrice = markPrice;
        this.realisedPnl = realisedPnl;
        this.unrealisedPnl = unrealisedPnl;
        this.avgEntryPrice = avgEntryPrice;
        this.liquidationPrice = liquidationPrice;
        this.bankruptPrice = bankruptPrice;
        this.timestamp = timestamp;
    }

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
        return timestamp;
    }
}

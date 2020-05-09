package bitmex.entities;

public class TradeBinData {
    private String timestamp;
    private String symbol;
    private Float open;
    private Float high;
    private Float low;
    private Float close;
    private Long trades;
    private Long volume;
    private Float vwap;

    public TradeBinData() {}

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public Float getOpen() {
        return open;
    }

    public Float getHigh() {
        return high;
    }

    public Float getLow() {
        return low;
    }

    public Float getClose() {
        return close;
    }

    public Long getTrades() {
        return trades;
    }

    public Long getVolume() {
        return volume;
    }

    public Float getVwap() {
        return vwap;
    }
}

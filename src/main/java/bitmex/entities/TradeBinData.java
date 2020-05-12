package bitmex.entities;

public class TradeBinData {
    private final String timestamp;
    private final String symbol;
    private final Float open;
    private final Float high;
    private final Float low;
    private final Float close;
    private final Long trades;
    private final Long volume;
    private final Float vwap;

    public TradeBinData(String timestamp, String symbol, Float open, Float high, Float low, Float close, Long trades, Long volume, Float vwap) {
        this.timestamp = timestamp;
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.trades = trades;
        this.volume = volume;
        this.vwap = vwap;
    }

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

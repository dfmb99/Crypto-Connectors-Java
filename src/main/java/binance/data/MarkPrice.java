package binance.data;

public class MarkPrice {
    private String symbol;
    private Float markPrice, lastFundingRate;
    private Long nextFundingTime, time;

    public String getSymbol() {
        return symbol;
    }

    public Float getMarkPrice() {
        return markPrice;
    }

    public Float getLastFundingRate() {
        return lastFundingRate;
    }

    public Long getNextFundingTime() {
        return nextFundingTime;
    }

    public Long getTime() {
        return time;
    }
}

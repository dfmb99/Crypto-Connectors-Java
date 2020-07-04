package market_maker;

public class Instrument {
    public String symbol;
    private float midPrice;
    private float askPrice;
    private float bidPrice;
    private float fairPrice;
    private float fundingRate;
    private float highPrice;
    private float lowPrice;
    private float impactAskPrice;

    public float getImpactAskPrice() {
        return impactAskPrice;
    }

    public float getImpactBidPrice() {
        return impactBidPrice;
    }

    private float impactBidPrice;
    private boolean hasLiquidity;
    private long openInterest;

    public float getHighPrice() {
        return highPrice;
    }

    public float getLowPrice() {
        return lowPrice;
    }

    public long getOpenInterest() {
        return openInterest;
    }

    public boolean isHasLiquidity() {
        return hasLiquidity;
    }

    public float getMidPrice() {
        return midPrice;
    }

    public float getAskPrice() {
        return askPrice;
    }

    public float getBidPrice() {
        return bidPrice;
    }

    public float getFairPrice() {
        return fairPrice;
    }

    public float getFundingRate() {
        return fundingRate;
    }

    public String getSymbol() {
        return symbol;
    }

}

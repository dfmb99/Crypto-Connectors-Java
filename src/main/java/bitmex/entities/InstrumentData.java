package bitmex.entities;

public class InstrumentData {

    private final String symbol;
    private final String state;
    private final String expiry;
    private final Float tickSize;
    private final Long multiplier;
    private final Boolean isQuanto;
    private final Boolean isInverse;
    private final Float initMargin;
    private final Float maintMargin;
    private final Float makerFee;
    private final Float takerFee;
    private final String fundingTimestamp;
    private final Float fundingRate;
    private final Float indicativeFundingRate;
    private final Float vwap;
    private final Float bidPrice;
    private final Float midPrice;
    private final Float askPrice;
    private final Float impactBidPrice;
    private final Float impactMidPrice;
    private final Float impactAskPrice;
    private final Long openInterest;
    private final Long openValue;
    private final Float fairBasisRate;
    private final Float fairBasis;
    private final Float fairPrice;
    private final Float markPrice;
    private final String timestamp;

    public InstrumentData(String symbol, String state, String expiry, Float tickSize, Long multiplier, Boolean isQuanto, Boolean isInverse, Float initMargin, Float maintMargin, Float makerFee, Float takerFee, String fundingTimestamp, Float fundingRate, Float indicativeFundingRate, Float vwap, Float bidPrice, Float midPrice, Float askPrice, Float impactBidPrice, Float impactMidPrice, Float impactAskPrice, Long openInterest, Long openValue, Float fairBasisRate, Float fairBasis, Float fairPrice, Float markPrice, String timestamp) {
        this.symbol = symbol;
        this.state = state;
        this.expiry = expiry;
        this.tickSize = tickSize;
        this.multiplier = multiplier;
        this.isQuanto = isQuanto;
        this.isInverse = isInverse;
        this.initMargin = initMargin;
        this.maintMargin = maintMargin;
        this.makerFee = makerFee;
        this.takerFee = takerFee;
        this.fundingTimestamp = fundingTimestamp;
        this.fundingRate = fundingRate;
        this.indicativeFundingRate = indicativeFundingRate;
        this.vwap = vwap;
        this.bidPrice = bidPrice;
        this.midPrice = midPrice;
        this.askPrice = askPrice;
        this.impactBidPrice = impactBidPrice;
        this.impactMidPrice = impactMidPrice;
        this.impactAskPrice = impactAskPrice;
        this.openInterest = openInterest;
        this.openValue = openValue;
        this.fairBasisRate = fairBasisRate;
        this.fairBasis = fairBasis;
        this.fairPrice = fairPrice;
        this.markPrice = markPrice;
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getState() {
        return state;
    }

    public String getExpiry() {
        return expiry;
    }

    public Float getTickSize() {
        return tickSize;
    }

    public Long getMultiplier() {
        return multiplier;
    }

    public Boolean getQuanto() {
        return isQuanto;
    }

    public Boolean getInverse() {
        return isInverse;
    }

    public Float getInitMargin() {
        return initMargin;
    }

    public Float getMaintMargin() {
        return maintMargin;
    }

    public Float getMakerFee() {
        return makerFee;
    }

    public Float getTakerFee() {
        return takerFee;
    }

    public String getFundingTimestamp() {
        return fundingTimestamp;
    }

    public Float getFundingRate() {
        return fundingRate;
    }

    public Float getIndicativeFundingRate() {
        return indicativeFundingRate;
    }

    public Float getVwap() {
        return vwap;
    }

    public Float getBidPrice() {
        return bidPrice;
    }

    public Float getMidPrice() {
        return midPrice;
    }

    public Float getAskPrice() {
        return askPrice;
    }

    public Float getImpactBidPrice() {
        return impactBidPrice;
    }

    public Float getImpactMidPrice() {
        return impactMidPrice;
    }

    public Float getImpactAskPrice() {
        return impactAskPrice;
    }

    public Long getOpenInterest() {
        return openInterest;
    }

    public Long getOpenValue() {
        return openValue;
    }

    public Float getFairBasisRate() {
        return fairBasisRate;
    }

    public Float getFairBasis() {
        return fairBasis;
    }

    public Float getFairPrice() {
        return fairPrice;
    }

    public Float getMarkPrice() {
        return markPrice;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

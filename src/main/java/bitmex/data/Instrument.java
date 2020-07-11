package bitmex.data;

public class Instrument {
    private String timestamp;
    private Float markPrice;
    private Float midPrice;
    private Float bidPrice;
    private Float askPrice;
    private Float tickSize;
    private Float underlyingToSettleMultiplier;
    private Float multiplier;
    private Float indicativeSettlePrice;

    private Float initMargin;
    private Boolean isQuanto, isInverse;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Float getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(Float fairPrice) {
        this.markPrice = fairPrice;
    }

    public Float getMidPrice() {
        return midPrice;
    }

    public void setMidPrice(Float midPrice) {
        this.midPrice = midPrice;
    }

    public Float getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(Float bidPrice) {
        this.bidPrice = bidPrice;
    }

    public Float getAskPrice() {
        return askPrice;
    }

    public void setAskPrice(Float askPrice) {
        this.askPrice = askPrice;
    }

    public Float getTickSize() {
        return tickSize;
    }

    public void setTickSize(Float tickSize) {
        this.tickSize = tickSize;
    }

    public Float getUnderlyingToSettleMultiplier() {
        return underlyingToSettleMultiplier;
    }

    public void setUnderlyingToSettleMultiplier(Float underlyingToSettleMultiplier) {
        this.underlyingToSettleMultiplier = underlyingToSettleMultiplier;
    }

    public Float getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Float multiplier) {
        this.multiplier = multiplier;
    }

    public Float getIndicativeSettlePrice() {
        return indicativeSettlePrice;
    }

    public void setIndicativeSettlePrice(Float indicativeSettlePrice) {
        this.indicativeSettlePrice = indicativeSettlePrice;
    }

    public Boolean getQuanto() {
        return isQuanto;
    }

    public void setQuanto(Boolean quanto) {
        isQuanto = quanto;
    }

    public Boolean getInverse() {
        return isInverse;
    }

    public void setInverse(Boolean inverse) {
        isInverse = inverse;
    }

    public Float getInitMargin() {
        return initMargin;
    }

    /**
     * Updates current object with other object data
     *
     * @param other - updated object
     */
    public void update(Instrument other) {
        if(other.getTimestamp() != null)
            this.timestamp = other.getTimestamp();
        if(other.getMarkPrice() != null)
            this.markPrice = other.getMarkPrice();
        if(other.getMidPrice() != null)
            this.midPrice = other.getMidPrice();
        if(other.getBidPrice() != null)
            this.bidPrice = other.getBidPrice();
        if(other.getAskPrice() != null)
            this.askPrice = other.getAskPrice();
    }
}

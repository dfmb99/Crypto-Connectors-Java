package bitmex.data;

public class Instrument {
    private Float fairPrice, midPrice, bidPrice, askPrice;

    public Float getFairPrice() {
        return fairPrice;
    }

    public void setFairPrice(Float fairPrice) {
        this.fairPrice = fairPrice;
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
}

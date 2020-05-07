package bitmex.ws.entities;

import java.lang.reflect.Field;

public class InstrumentData {

    public final String symbol;
    public String state;
    public String expiry;
    public float tickSize;
    public long multiplier;
    public boolean isQuanto;
    public boolean isInverse;
    public float initMargin;
    public float maintMargin;
    public float makerFee;
    public float takerFee;
    public String fundingTimestamp;
    public float fundingRate;
    public float indicativeFundingRate;
    public float vwap;
    public float bidPrice;
    public float midPrice;
    public float askPrice;
    public float impactBidPrice;
    public float impactMidPrice;
    public float impactAskPrice;
    public long openInterest;
    public long openValue;
    public float fairBasisRate;
    public float fairBasis;
    public float fairPrice;
    public float markPrice;
    public String timestamp;

    public InstrumentData(String symbol, String state, String expiry, float tickSize, long multiplier, boolean isQuanto, boolean isInverse, float initMargin, float maintMargin, float makerFee, float takerFee, String fundingTimestamp, float fundingRate, float indicativeFundingRate, float vwap, float bidPrice, float midPrice, float askPrice, float impactBidPrice, float impactMidPrice, float impactAskPrice, long openInterest, long openValue, float fairBasisRate, float fairBasis, float fairPrice, float markPrice, String timestamp) {

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

    /**
     * Updated this object with the fields of other object if they are not null
     * @param other - other object
     */
    public void update(InstrumentData other) {
        Field[] fields = other.getClass().getDeclaredFields();
        for(Field field: fields) {
            // type of the field
            Class<?> type = field.getType();
            try {
                Object otherValue = field.get(type);
                Field thisField = this.getClass().getDeclaredField(field.getName());
                if(otherValue != null && !otherValue.equals(thisField.get(type)))
                    thisField.set(type, otherValue);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                // Do nothing
            }
        }
    }
}

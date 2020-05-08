package bitmex.entities;

import java.lang.reflect.Field;

public class InstrumentData {

    private String symbol;
    private String state;
    private String expiry;
    private Float tickSize;
    private Long multiplier;
    private Boolean isQuanto;
    private Boolean isInverse;
    private Float initMargin;
    private Float maintMargin;
    private Float makerFee;
    private Float takerFee;
    private String fundingTimestamp;
    private Float fundingRate;
    private Float indicativeFundingRate;
    private Float vwap;
    private Float bidPrice;
    private Float midPrice;
    private Float askPrice;
    private Float impactBidPrice;
    private Float impactMidPrice;
    private Float impactAskPrice;
    private Long openInterest;
    private Long openValue;
    private Float fairBasisRate;
    private Float fairBasis;
    private Float fairPrice;
    private Float markPrice;
    private String timestamp;

    public InstrumentData() { }

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

    public long getTimestamp() {
        return TimeStamp.getTimestamp(timestamp);
    }

    /**
     * Updated this object with the fields of other object if they are not null
     * @param other - other object
     */
    public void updateObjMemory(InstrumentData other) {
        Field[] fields = other.getClass().getDeclaredFields();
        for(Field field: fields) {
            try {
                Object otherValue = field.get(other);
                Field thisField = this.getClass().getDeclaredField(field.getName());
                if(otherValue != null && !otherValue.equals(thisField.get(this)))
                    thisField.set(this, otherValue);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                // Do nothing
            }
        }
    }
}

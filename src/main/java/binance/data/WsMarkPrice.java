package binance.data;

public class WsMarkPrice {

    private String e, s;
    private Long E, T;
    private Float p, r;

    public String getEventType() {
        return e;
    }

    public Long getNextFundingTime() {
        return T;
    }
    public Long getEventTime() {
        return E;
    }

    public Float getMarkPrice() {
        return p;
    }

    public Float getFundingRate() {
        return r;
    }

    public String getSymbol() {
        return s;
    }
}

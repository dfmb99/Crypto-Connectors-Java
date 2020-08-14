package binance.data;

public class WsMiniTicker {

    private Long E;
    private String e, s;
    private Float w, c, o, h, l , v, q, n;

    public String getEventType() {
        return e;
    }

    public Long getEventTime() {
        return E;
    }

    public String getSymbol() {
        return s;
    }

    public Float getWeightedAvgPrice() {
        return w;
    }

    public Float getLastPrice() {
        return c;
    }

    public Float getOpenPrice() {
        return o;
    }

    public Float getHighPrice() {
        return h;
    }

    public Float getLowPrice() {
        return l;
    }

    public Float getBaseAssetVolume() {
        return v;
    }

    public Float getQuoteAssetVolume() {
        return q;
    }

    public Float getNumTrade() {
        return n;
    }
}

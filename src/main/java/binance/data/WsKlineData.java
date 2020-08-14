package binance.data;

public class WsKlineData {
    private Long t, T, n;
    private String s, i;
    private Float o,c, h, l, v, q;
    private boolean x;

    public Long getStartTime() {
        return t;
    }

    public Long getCloseTime() {
        return T;
    }

    public Long getNumTrades() {
        return n;
    }

    public String getSymbol() {
        return s;
    }

    public String getInterval() {
        return i;
    }

    public Float getOpenPrice() {
        return o;
    }

    public Float getClosePrice() {
        return c;
    }

    public Float getHighPrice() {
        return h;
    }

    public Float getLowPrice() {
        return l;
    }

    public Float getVolume() {
        return v;
    }

    public Float getQuoteVolume() {
        return q;
    }

    public boolean isKlineClosed() {
        return x;
    }
}

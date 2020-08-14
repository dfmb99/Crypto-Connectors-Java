package binance.data;

public class WsAggTrade {
    private String e, s;
    private Float p, q;
    private Long E, a, T, f ,l;
    private boolean m;

    public String getEventType() {
        return e;
    }

    public Long getAggTradeID() {
        return a;
    }

    public Long getTradeTime() {
        return T;
    }

    public Long getFirstTradeID() {
        return f;
    }

    public Long getLastTradeID() {
        return l;
    }

    public boolean isMarketMaker() {
        return m;
    }

    public String getSymbol() {
        return s;
    }

    public Float getPrice() {
        return p;
    }

    public Float getQuantity() {
        return q;
    }
}

package binance.data;

public class WsKlineRec {
    private String e, s;
    private Long E;
    private WsKlineData k;

    public String getEventType() {
        return e;
    }

    public Long getEventTime() {
        return E;
    }

    public WsKlineData getKline() {
        return k;
    }

    public String getSymbol() {
        return s;
    }
}

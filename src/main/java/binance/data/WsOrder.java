package binance.data;

public class WsOrder {

    private String e;
    private Long E, T;
    private WsOrderData o;

    public String getEventType() {
        return e;
    }

    public Long getEventTime() {
        return E;
    }

    public Long getTransactionTime() {
        return T;
    }

    public WsOrderData getOrder() {
        return o;
    }
}

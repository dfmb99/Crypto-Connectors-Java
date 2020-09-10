package binance.data;

public class WsUserData {
    private String e;
    private Long E;

    public String getEventType() {
        return e;
    }

    public Long getEventTime() {
        return E;
    }
}

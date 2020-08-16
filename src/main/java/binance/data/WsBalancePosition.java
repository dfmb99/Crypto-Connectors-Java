package binance.data;

public class WsBalancePosition {
    private String e;
    private Long E, T;
    private WsBalancePositionData a;

    public String getEventType() {
        return e;
    }

    public Long getEventTime() {
        return E;
    }

    public Long getTransactionTime() { return T; }

    public WsBalancePositionData getBalancePositionData() { return a; }

    public int compareTo(WsBalancePosition other) {
        if(this.E > other.getEventTime())
            return 1;
        else if(other.getEventTime() > this.E)
            return -1;
        else
            return 0;
    }
}

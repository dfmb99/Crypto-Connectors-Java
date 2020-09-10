package binance.data;

public class WsMarginCall {
    private String e;
    private Long E;
    private Float cw;
    private WsMarginCallPosData[] p;

    public String getEventType() {
        return e;
    }

    public Long getEventTime() {
        return E;
    }

    public Float getCrossWalletBalance() {
        return cw;
    }

    public WsMarginCallPosData[] getMarginCallPosition() {
        return p;
    }
}

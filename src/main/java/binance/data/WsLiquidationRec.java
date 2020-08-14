package binance.data;

public class WsLiquidationRec {
    private String e;
    private Long E;
    private WsLiquidationData o;

    public String getEventType() {
        return e;
    }

    public Long getEventTime() {
        return E;
    }

    public WsLiquidationData getLiquidationData() {
        return o;
    }
}

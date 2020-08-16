package binance.data;

public class WsBalancePositionData {

    private String m;
    private WsBalanceData[] B;
    private WsPositionData[] P;

    public String getEventReasonType() {
        return m;
    }

    public WsBalanceData[] getBalances() {
        return B;
    }

    public WsPositionData[] getPositions() {
        return P;
    }
}

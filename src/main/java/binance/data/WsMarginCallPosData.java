package binance.data;

public class WsMarginCallPosData {
    private String s, ps, mt;
    private Float pa, mp, up, mm, iw;

    public String getSymbol() {
        return s;
    }

    public String getPositionSide() {
        return ps;
    }

    public String getMarginType() {
        return mt;
    }

    public Float getPositionAmount() {
        return pa;
    }

    public Float getMarkPrice() {
        return mp;
    }

    public Float getUnrealizedPNL() {
        return up;
    }

    public Float getMaintenanceMargin() {
        return mm;
    }

    public Float getIsolatedWallet() {
        return iw;
    }
}

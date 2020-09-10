package binance.data;

public class WsPositionData {
    private String s, mt, ps;
    private Float pa, ep, cr, up, iw;

    public String getSymbol() {
        return s;
    }

    public String getMarginType() {
        return mt;
    }

    public String getPositionSide() {
        return ps;
    }

    public Float getPositionAmnt() {
        return pa;
    }

    public Float getEntryPrice() {
        return ep;
    }

    public Float getRealizedPNL() {
        return cr;
    }

    public Float getUnrealizedPNL() {
        return up;
    }

    public Float getIsolatedWallet() {
        return iw;
    }
}

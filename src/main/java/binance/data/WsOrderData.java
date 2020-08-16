package binance.data;

public class WsOrderData {

    private String s, c, S, o, f, X, i, wt, ot, ps;
    private Float q, p, ap, sp, l, AP, rp, z;
    private boolean m, R;

    public String getSymbol() {
        return s;
    }

    public String getSide() {
        return S;
    }

    public String getOrderType() {
        return o;
    }

    public String getTimeInForce() {
        return f;
    }

    public String getOrderStatus() {
        return X;
    }

    public String getOrderID() {
        return i;
    }

    public String getStopPriceWorkingType() {
        return wt;
    }

    public String getOrigOrderType() {
        return ot;
    }

    public String getPositionSide() {
        return ps;
    }

    public Float getOrigQty() {
        return q;
    }

    public Float getOrigPrice() {
        return p;
    }

    public Float getAvgPrice() {
        return ap;
    }

    public Float getStopPrice() {
        return sp;
    }

    public Float getLastOrdFilledQty() {
        return l;
    }

    public Float getAccumOrdFilledQty() {
        return z;
    }

    public Float getStopMarketOrdActivationPrice() {
        return AP;
    }

    public Float getRealizedProfit() {
        return rp;
    }

    public boolean isMakerSide() {
        return m;
    }

    public boolean isReduceOnly() {
        return R;
    }

    public String getClientOrderID() {
        return c;
    }
}

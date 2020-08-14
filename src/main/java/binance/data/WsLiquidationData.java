package binance.data;

public class WsLiquidationData {

    private String s, S, o, f, X;
    private Float q, p, ap, l, z;
    private Long T;

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

    public Float getQuantity() {
        return q;
    }

    public Float getPrice() {
        return p;
    }

    public Float getAvgPrice() {
        return ap;
    }

    public Float getLastFilledQty() {
        return l;
    }

    public Float getAccumQtyFilled() {
        return z;
    }

    public Long getOrderTime() {
        return T;
    }
}

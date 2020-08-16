package binance.data;

public class WsBalanceData {

    private String a;
    private Float wb, cw;

    public String getAsset() {
        return a;
    }

    public Float getWalletBalance() {
        return wb;
    }

    public Float getCrossWallet() {
        return cw;
    }
}

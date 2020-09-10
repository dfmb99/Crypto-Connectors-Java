package binance.data;

public class AccountBalance {
    private String accountAlias, asset;
    private Float balance, crossWalletBalance, crossUnPnl, availableBalance, maxWithdrawAmount;

    public String getAccountAlias() {
        return accountAlias;
    }

    public String getAsset() {
        return asset;
    }

    public Float getBalance() {
        return balance;
    }

    public Float getCrossWalletBalance() {
        return crossWalletBalance;
    }

    public Float getCrossUnPnl() {
        return crossUnPnl;
    }

    public Float getAvailableBalance() {
        return availableBalance;
    }

    public Float getMaxWithdrawAmount() {
        return maxWithdrawAmount;
    }
}

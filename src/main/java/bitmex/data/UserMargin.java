package bitmex.data;

public class UserMargin {
    private Float walletBalance;

    public Float getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(Float walletBalance) {
        this.walletBalance = walletBalance;
    }

    /**
     * Updates current object with other object data
     *
     * @param other - updated object
     */
    public void update(UserMargin other) {
        if(other.getWalletBalance() != null)
            this.walletBalance = other.getWalletBalance();
    }
}

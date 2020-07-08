package bitmex.data;

public class UserMargin {
    private Float marginBalance, availableMargin, walletBalance;

    public Float getMarginBalance() {
        return marginBalance;
    }

    public void setMarginBalance(Float marginBalance) {
        this.marginBalance = marginBalance;
    }

    public Float getAvailableMargin() {
        return availableMargin;
    }

    public void setAvailableMargin(Float availableMargin) {
        this.availableMargin = availableMargin;
    }

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

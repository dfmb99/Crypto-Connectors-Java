package bitmex.entities;

public class MarginData {
    private final Long amount;
    private final Long initMargin;
    private final Long maintMargin;
    private final Long realisedPnl;
    private final Long unrealisedPnl;
    private final Long walletBalance;
    private final Long marginBalance;
    private final Long excessMargin;
    private final Long availableMargin;
    private final String timestamp;

    public MarginData(Long amount, Long initMargin, Long maintMargin, Long realisedPnl, Long unrealisedPnl, Long walletBalance, Long marginBalance, Long excessMargin, Long availableMargin, String timestamp) {
        this.amount = amount;
        this.initMargin = initMargin;
        this.maintMargin = maintMargin;
        this.realisedPnl = realisedPnl;
        this.unrealisedPnl = unrealisedPnl;
        this.walletBalance = walletBalance;
        this.marginBalance = marginBalance;
        this.excessMargin = excessMargin;
        this.availableMargin = availableMargin;
        this.timestamp = timestamp;
    }

    public Long getAmount() {
        return amount;
    }

    public Long getInitMargin() {
        return initMargin;
    }

    public Long getMaintMargin() {
        return maintMargin;
    }

    public Long getRealisedPnl() {
        return realisedPnl;
    }

    public Long getUnrealisedPnl() {
        return unrealisedPnl;
    }

    public Long getWalletBalance() {
        return walletBalance;
    }

    public Long getMarginBalance() {
        return marginBalance;
    }

    public Long getExcessMargin() {
        return excessMargin;
    }

    public Long getAvailableMargin() {
        return availableMargin;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

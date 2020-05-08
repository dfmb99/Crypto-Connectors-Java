package bitmex.entities;

public class MarginData {
    private Long amount;
    private Long initMargin;
    private Long maintMargin;
    private Long realisedPnl;
    private Long unrealisedPnl;
    private Long walletBalance;
    private Long marginBalance;
    private Long excessMargin;
    private Long availableMargin;
    private String timestamp;

    public MarginData() { }

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

    public long getTimestamp() {
        return TimeStamp.getTimestamp(timestamp);
    }
}

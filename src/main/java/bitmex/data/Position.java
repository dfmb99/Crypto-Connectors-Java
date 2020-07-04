package bitmex.data;

public class Position {
    private Float avgEntryPrice;
    private Long currentQty;

    public Float getAvgEntryPrice() {
        return avgEntryPrice;
    }

    public void setAvgEntryPrice(Float avgEntryPrice) {
        this.avgEntryPrice = avgEntryPrice;
    }

    public Long getCurrentQty() {
        return currentQty;
    }

    public void setCurrentQty(Long currentQty) {
        this.currentQty = currentQty;
    }
}

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

    /**
     * Updates current object with other object data
     *
     * @param other - updated object
     */
    public void update(Position other) {
        if(other.getAvgEntryPrice() != null)
            this.avgEntryPrice = other.getAvgEntryPrice();
        if(other.getCurrentQty() != null)
            this.currentQty = other.getCurrentQty() ;
    }
}

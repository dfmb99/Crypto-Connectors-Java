package bitmex.data;

public class Order {
    private String orderID;
    private String clOrdID;
    private String side;
    private String ordType;
    private String ordStatus;
    private Long orderQty;
    private Float price;

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getClOrdID() {
        return clOrdID;
    }

    public void setClOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getOrdType() {
        return ordType;
    }

    public void setOrdType(String ordType) {
        this.ordType = ordType;
    }

    public Long getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(Long orderQty) {
        this.orderQty = orderQty;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public String getOrdStatus() {
        return ordStatus;
    }

    public void setOrdStatus(String ordStatus) {
        this.ordStatus = ordStatus;
    }

    public boolean equals(Order other) {
        return this.orderID.equals(other.getOrderID());
    }

    /**
     * Updates current object with other object data
     *
     * @param other - updated object
     */
    public void update(Order other) {
        if(other.getSide() != null)
            this.side = other.getSide();
        if(other.getOrdType() != null)
            this.ordType = other.getOrdType();
        if(other.getOrdStatus() != null)
            this.ordStatus = other.getOrdStatus();
        if(other.getOrderQty() != null)
            this.orderQty = other.getOrderQty();
        if(other.getPrice() != null)
            this.price = other.getPrice();
    }
}

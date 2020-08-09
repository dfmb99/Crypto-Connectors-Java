package binance.data;

public class Income {
    private String symbol, incomeType, asset, info;
    private Float income;
    private Long time;

    public String getSymbol() {
        return symbol;
    }

    public String getIncomeType() {
        return incomeType;
    }

    public String getAsset() {
        return asset;
    }

    public String getInfo() {
        return info;
    }

    public Float getIncome() {
        return income;
    }

    public Long getTime() {
        return time;
    }
}

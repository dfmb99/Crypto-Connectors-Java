package binance.data;

public class Constants {

    enum ReasonType {
        DEPOSIT,
        WITHDRAW,
        ORDER,
        FUNDING_FEE,
        WITHDRAW_REJECT,
        ADJUSTMENT,
        INSURANCE_CLEAR,
        ADMIN_DEPOSIT,
        ADMIN_WITHDRAW,
        MARGIN_TRANSFER,
        MARGIN_TYPE_CHANGE,
        ASSET_TRANSFER,
        OPTIONS_PREMIUM_FEE,
        OPTIONS_SETTLE_PROFIT
    }

    enum Side {
        BUY,
        SELL
    }

    enum OrderType {
        MARKET,
        LIMIT,
        STOP,
        TAKE_PROFIT,
        LIQUIDATION
    }

    enum ExecutionType {
        NEW,
        PARTIAL_FILL,
        FILL,
        CANCELED,
        CALCULATED,
        EXPIRED,
        TRADE
    }

    enum OrderStatus {
        NEW,
        PARTIALLY_FILLED,
        FILLED,
        CANCELED,
        EXPIRED,
        NEW_INSURANCE,
        NEW_ADL
    }

    enum TimeInForce {
        GTC,
        IOC,
        FOK,
        GTX
    }

    enum WorkingType {
        MARK_PRICE,
        CONTRACT_PRICE
    }
}

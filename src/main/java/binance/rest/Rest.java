package binance.rest;

import binance.data.MarkPrice;
import binance.data.Order;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public interface Rest {
    //Rest endpoints and path
    String REST_USDT_FUTURES = "https://fapi.binance.com";
    String REST_USDT_FUTURES_TESTNET = "https://testnet.binancefuture.com";

    //Server configuration
    int CONNECTION_TIMEOUT = 3000;
    int REPLY_TIMEOUT = 3000;
    int RETRY_PERIOD = 3000;


    /**
     * Returns Mark Price and Funding Rate
     * @param symbol - not mandatory
     * @return rest response, null if error
     */
    MarkPrice get_mark_price(@NotNull String symbol);

    /**
     * Returns Kline/candlestick bars for a symbol. Klines are uniquely identified by their open time.
     * @param params - parameters data
     * @return rest response, null if error
     */
    JsonArray get_klines(@NotNull JsonObject params);

    /**
     * Send in a new order.
     * @param params - parameters data
     * @return rest response, null if error
     */
    Order place_order(JsonObject params);

    /**
     * Send in a new order.
     * @param batchOrders - batchOrders data
     * @return rest response, null if error
     */
    Order[] place_batched_orders(JsonArray batchOrders);

    /**
     * Change user's position mode (Hedge Mode or One-way Mode ) on EVERY symbol
     * @param dualSidePosition - "true": Hedge Mode mode; "false": One-way Mode
     */
    void change_position_mode(@NotNull String dualSidePosition);


}

package bitmex;

import com.google.gson.JsonObject;

/**
 * Interface to connect to the Bitmex Rest API, see more at https://www.bitmex.com/api/explorer/
 */
public interface Rest {
    String url = "https://testnet.bitmex.com";
    String apiPath = "/api/v1";
    int CONNECTION_TIMEOUT = 2000;
    int REPLY_TIMEOUT = 2000;
    int RETRY_PERIOD = 3000;

    /**
     * Get all raw executions for your account
     * Calls the GET/execution method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String get_execution(JsonObject data);

    /**
     * Get instruments
     * Calls the GET/instrument method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String get_instrument(JsonObject data);

    /**
     * Get your orders
     * Calls the GET/order method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String get_order(JsonObject data);

    /**
     * Amend the quantity or price of an open order
     * Calls the PUT/order method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONObject
     *         error message otherwise
     */
    String put_order(JsonObject data);

    /**
     * Create a new order
     * Calls the POST/order method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONObject
     *         error message otherwise
     */
    String post_order(JsonObject data);


    /**
     * Cancel order(s). Send multiple order IDs to cancel in bulk
     * Calls the DELETE/order method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String del_order(JsonObject data);

    /**
     * Cancels all of your orders
     * Calls the DELETE/order/all method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String del_order_all(JsonObject data);

    /**
     * Amend multiple orders for the same symbol
     * Calls the PUT/order/bulk method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String put_order_bulk(JsonObject data);

    /**
     * Create multiple new orders for the same symbol
     * Calls the POST/order/bulk method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String post_order_bulk(JsonObject data);

    /**
     * Automatically cancel all your orders after a specified timeout
     * Calls the POST/order/cancelAllAfter method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONObject
     *         error message otherwise
     */
    String post_order_cancelAllAfter(JsonObject data);

    /**
     * Get your positions
     * Calls the GET/position method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String get_position(JsonObject data);

    /**
     * Get previous trades in time buckets
     * Calls the GET/trade/bucketed method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String get_trade_bucketed(JsonObject data);

    /**
     * Get your account's margin status. Send a currency of "all" to receive an array of all supported currencies
     * Calls the GET/user/margin method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONObject or JSONArray depending on data body
     *         error message otherwise
     */
    String get_user_margin(JsonObject data);

    /**
     * Get a history of all of your wallet transactions (deposits, withdrawals, PNL)
     * Calls the GET/user/walletHistory method on server
     * @param data - data sent to server as parameters
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String get_user_walletHistory(JsonObject data);

    /**
     * Get 7 days worth of Quote Fill Ratio statistics
     * Calls the GET/user/quoteFillRatio method on server
     * @return server response that can be parsed to JSONArray
     *         error message otherwise
     */
    String get_user_quoteFillRatio();

}

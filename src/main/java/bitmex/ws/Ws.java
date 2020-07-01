package bitmex.ws;

import com.google.gson.JsonArray;
import market_maker.Settings;

import javax.websocket.ClientEndpoint;

@ClientEndpoint
public interface Ws {
    //WebSocket endpoints
    String WS_TESTNET = "wss://testnet.bitmex.com/realtime";
    String WS_MAINNET = "wss://www.bitmex.com/realtime";

    int LIQ_MAX_LEN = 50;
    int TRADE_BIN_MAX_LEN = Settings.SPREAD_SNAPS;
    int EXEC_MAX_LEN = 50;
    int ORDER_MAX_LEN = 50;
    int RETRY_PERIOD = 5000;
    int MAX_LATENCY = 15000;
    int FORCE_RECONNECT_INTERVAL = 60000;

    /**
     * Returns true if websocket connection is open, false otherwise
     * @return true if websocket connection is open, false otherwise
     */
    boolean isSessionOpen();

    /**
     * Closes current suer session if one is open
     * @return true if session closed with success, false otherwise
     */
    boolean closeSession();

    /**
     * Gets size of level2 orderBook row with price == 'price'
     *
     * @param price - price of row to query
     * @return size - size of orderBook row if data is available
     * -1 otherwise
     */
    long getL2Size(float price);

    /**
     * Returns open liquidations on
     * @return JsonArray data
     */
    JsonArray get_open_liquidation();

    /**
     * Returns instrument data
     * @return JsonArray data
     */
    JsonArray get_instrument();

    /**
     * Returns tradeBin1m data
     * @return JsonArray data
     */
    JsonArray get_trabeBin1m();

    /**
     * Return orderBookL2 data
     * @return JsonArray data
     */
    JsonArray get_orderBookL2();
    /**
     * Return margin data
     * @return JsonArray data
     */
    JsonArray get_margin();

    /**
     * Returns execution data
     * @return JsonArray data
     */
    JsonArray get_execution();

    /**
     * Returns position data
     * @return JsonArray data
     */
    JsonArray get_position();

    /**
     * Returns open orders
     * @return JsonArray data
     */
    JsonArray get_openOrders(String orderIDPrefix);

    /**
     * Returns filled orders
     * @return JsonArray data
     */
    JsonArray get_filledOrders(String orderIDPrefix);

    /**
     * Returns last order filled price
     * @return last order filled price
     */
    float get_price_last_order_filled();
}

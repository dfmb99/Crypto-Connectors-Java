package bitmex.ws;

import bitmex.data.*;

import javax.websocket.ClientEndpoint;

@ClientEndpoint
public interface Ws {
    //Constants
    String INSTRUMENT = "instrument";
    String ORDER_BOOK_L2 = "orderBookL2";
    String LIQUIDATION = "liquidation";
    String MARGIN = "margin";
    String POSITION = "position";
    String TRADE_BIN = "tradeBin1m";
    String EXECUTION = "execution";
    String ORDER = "order";

    //WebSocket endpoints
    String WS_TESTNET = "wss://testnet.bitmex.com/realtime";
    String WS_MAINNET = "wss://www.bitmex.com/realtime";

    // Memory maximum size settings
    int LIQ_MAX_LEN = 100;
    int TRADE_BIN_MAX_LEN = 100;
    int EXEC_MAX_LEN = 100;
    int ORDER_MAX_LEN = 100;

    //Server configuration
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
     * Returns instrument data
     * @return Instrument data
     */
    Instrument get_instrument();

    /**
     * Returns open liquidations data
     * @return Liquidation data
     */
    Liquidation[] get_open_liquidation();

    /**
     * Returns tradeBin1m data
     * @return TradeBin data
     */
    TradeBin[] get_trabeBin1m();

    /**
     * Return orderBookL2 data
     * @return OrderBookL2 data
     */
    OrderBookL2[] get_orderBookL2();

    /**
     * Returns size of orderbook level with price == 'price'
     * @param price - price of level
     * @return size of the orderbook level
     */
    long getL2Size(float price);

    /**
     * Return margin data
     * @return UserMargin data
     */
    UserMargin get_margin();

    /**
     * Returns execution data
     * @return Execution[] data
     */
    Execution[] get_execution();

    /**
     * Returns position data
     * @return Position data
     */
    Position get_position();

    /**
     * Returns open orders
     *
     * @param orderIDPrefix - orderID prefix
     * @return Order[] data
     */
    Order[] get_openOrders(String orderIDPrefix);

    /**
     * Returns filled orders
     *
     * @param orderIDPrefix - orderID prefix
     * @return Order[] data
     */
    Order[] get_filledOrders(String orderIDPrefix);
}

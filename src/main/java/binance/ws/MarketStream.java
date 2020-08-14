package binance.ws;

import javax.websocket.ClientEndpoint;

@ClientEndpoint
public interface MarketStream {
    //WebSocket endpoints
    String WS_TESTNET = "wss://stream.binancefuture.com";
    String WS_MAINNET = "wss://fstream.binance.com";
    int MAX_LATENCY = 15000;
    int FORCE_RECONNECT_INTERVAL = 60000;

    //Streams
    String KLINE_1M = "kline_1m";
    String MINI_TICKER = "miniTicker";
    String AGG_TRADE = "aggTrade";
    String MARK_PRICE = "markPrice";
    String LIQUIDATION = "forcedOrder";

    int MAX_LEN_KLINE = 100;
    int MAX_LEN_AGG_TRADE = 100;
    int MAX_LEN_LIQUIDATION = 100;

    //Server configuration
    int RETRY_PERIOD = 5000;

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
     * Gets mark price of contract
     * @return markPrice
     */
    float get_mark_price();

    /**
     * Gets funding rate of contract
     * @return funding rate
     */
    float get_funding_rate();

    /**
     * Retruns timestamp of next funding period
     * @return timestamp of next funding rate
     */
    long get_next_funding_period();



}

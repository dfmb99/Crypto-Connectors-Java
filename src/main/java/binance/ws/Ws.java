package binance.ws;

import javax.websocket.ClientEndpoint;

@ClientEndpoint
public interface Ws {
    //WebSocket endpoints
    String WS_TESTNET = "wss://stream.binancefuture.com";
    String WS_MAINNET = "wss://fstream.binance.com";

    //Streams
    String KLINE_1M = "kline_1m";
    String MINI_TICKER = "miniTicker";
    String AGG_TRADE = "aggTrade";
    String MARK_PRICE = "markPrice";
    String LIQUIDATION = "forcedOrder";

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

}

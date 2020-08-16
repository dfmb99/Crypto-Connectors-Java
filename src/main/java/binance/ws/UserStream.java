package binance.ws;

import javax.websocket.ClientEndpoint;

@ClientEndpoint
public interface UserStream {
    //WebSocket endpoints
    String WS_TESTNET = "wss://stream.binancefuture.com";
    String WS_MAINNET = "wss://fstream.binance.com";
    int MAX_LATENCY = 15000;
    int FORCE_RECONNECT_INTERVAL = 60000;

    //Streams
    String LISTEN_KEY_EXPIRED = "listenKeyExpired";
    String MARGIN_CALL = "MARGIN_CALL";
    String ACCOUNT_UPDATE = "ACCOUNT_UPDATE";
    String ORDER_TRADE_UPDATE = "ORDER_TRADE_UPDATE";

    int MAX_LEN_MARGIN_CALL = 100;
    int MAX_LEN_ACCOUNT = 100;
    int MAX_LEN_ORDER = 100;

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

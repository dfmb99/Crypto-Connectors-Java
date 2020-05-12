package bitmex.ws;

import javax.websocket.ClientEndpoint;

@ClientEndpoint
public interface Ws {
    //WebSocket endpoints
    String WS_TESTNET = "wss://testnet.bitmex.com/realtime";
    String WS_MAINNET = "wss://www.bitmex.com/realtime";

    int LIQ_MAX_LEN = 50;
    int TRADE_BIN_MAX_LEN = 200;
    int EXEC_MAX_LEN = 10;
    int RETRY_PERIOD = 3000;
}

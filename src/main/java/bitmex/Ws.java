package bitmex;

import javax.websocket.ClientEndpoint;

@ClientEndpoint
public interface Ws {
    int MAX_TABLE_LEN = 200;
    int RETRY_PERIOD = 3000;
}

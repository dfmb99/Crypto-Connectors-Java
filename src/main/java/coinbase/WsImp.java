package coinbase;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import utils.TimeStamp;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Heartbeat thread that sends ping messages to the websocket server
 */
class HeartbeatThread extends Thread {
    private final WsImp ws;
    private final static Logger LOGGER = Logger.getLogger(HeartbeatThread.class.getName());

    public HeartbeatThread(WsImp ws) {
        this.ws = ws;
        this.start();
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        while (!Thread.interrupted()) {
            if (System.currentTimeMillis() - startTime > 5000) {
                LOGGER.fine("Heartbeat thread reconnecting.");
                this.ws.closeSession();
                this.interrupt();
            }
        }
    }
}

@ClientEndpoint
public class WsImp {

    private final static Logger LOGGER = Logger.getLogger(WsImp.class.getName());
    private final static String URL = "wss://ws-feed.pro.coinbase.com";
    private static final int MAX_LATENCY = 15000;
    private final static int RETRY_PERIOD = 5000;
    private final static int FORCE_RECONNECT_INTERVAL = 60000;

    private final Gson g;
    private final WebSocketContainer container;
    private HeartbeatThread heartbeatThread;
    private Session userSession;
    private final String symbol;
    // last price of ticker
    private float lastPrice;
    // last sequence number of ticker
    private long seqNum;
    // allowed reconnect timestamp
    private Long reconnectStamp;

    /**
     * Coinbase web socket client implementation for one symbol
     */
    public WsImp(String symbol) {
        this.g = new Gson();
        this.container = ContainerProvider.getWebSocketContainer();
        this.heartbeatThread = null;
        this.lastPrice = -1f;
        this.seqNum = -1L;
        this.symbol = symbol;
        this.reconnectStamp = 0L;
        this.connect();
        this.waitForData();
    }

    /**
     * Connects to Coinbase web socket server
     */
    void connect() {
        try {
            this.container.connectToServer(this, URI.create(URL));
        } catch (Exception e) {
            LOGGER.warning("Failed to connect to web socket server.");
            try {
                Thread.sleep(RETRY_PERIOD);
            } catch (InterruptedException interruptedException) {
                // Do nothing
            }
            this.connect();
        }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        LOGGER.info(String.format("Connected to: %s", URL));
        this.userSession = userSession;
        this.heartbeatThread = new HeartbeatThread(this);
        this.sendMessage(String.format("{\"type\": \"subscribe\",\"product_ids\": [\"%s\"],\"channels\": [\"ticker\",\"heartbeat\"]}", this.symbol));
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param reason the reason for connection close
     */
    @OnClose
    public void onClose(CloseReason reason) {
        if (!this.heartbeatThread.isInterrupted())
            this.heartbeatThread.interrupt();
        this.heartbeatThread = null;
        LOGGER.warning(String.format("Websocket closed with code: %d", reason.getCloseCode().getCode()));
        this.userSession = null;
        this.seqNum = -1L;
        this.connect();
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        if (!this.heartbeatThread.isInterrupted())
            this.heartbeatThread.interrupt();
        this.heartbeatThread = new HeartbeatThread(this);
        JsonObject response = g.fromJson(message, JsonObject.class);
        String type = response.get("type").getAsString();

        if (type.equalsIgnoreCase("ticker")) {
            LOGGER.fine("Received ticker data.");
            new Thread(() -> update_ticker(response)).start();
        }
    }


    /**
     * Updates memory data when receives "trade" event
     *
     * @param data received
     */
    private void update_ticker(JsonObject data) {
        check_latency(data.get("time").getAsString());
        long newSeqNum = data.get("sequence").getAsLong();
        // if this update is more recent than the one we have in memory
        if (newSeqNum > this.seqNum) {
            this.seqNum = newSeqNum;
            this.lastPrice = data.get("price").getAsFloat();
        }
    }

    /**
     * Get last price
     */
    public float get_last_price() {
        return this.lastPrice;
    }

    /**
     * waits for instrument ws data, blocking thread
     */
    private void waitForData() {
        while (this.lastPrice < 0.0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    /**
     * Checks latency on a websocket update
     *
     * @param timestamp - timestamp last update
     */
    private void check_latency(String timestamp) {
        long updateTime = TimeStamp.getTimestamp(timestamp);
        long latency = System.currentTimeMillis() - updateTime;
        synchronized (reconnectStamp) {
            if (latency > MAX_LATENCY && System.currentTimeMillis() > reconnectStamp) {
                LOGGER.warning(String.format("Reconnecting to websocket due to high latency of: %d", latency));
                reconnectStamp = System.currentTimeMillis() + FORCE_RECONNECT_INTERVAL;
                this.closeSession();
            }
        }
    }

    /**
     * Callback hook for Error Events. This method will be invoked when a client receives a error.
     *
     * @param throwable - Error thrown
     */
    @OnError
    public void onError(Throwable throwable) {
        LOGGER.warning(throwable.toString());
        this.closeSession();
    }

    /**
     * Closes current suer session if one is open
     *
     * @return true if session closed with success, false otherwise
     */
    public boolean closeSession() {
        if (isSessionOpen()) {
            try {
                this.userSession.close();
                return true;
            } catch (IOException e) {
                LOGGER.warning("Could not close user session.");
                return false;
            }
        }
        return false;
    }

    /**
     * Gets current status of websocket connection
     *
     * @return true if websocket connection is open
     */
    public boolean isSessionOpen() {
        return this.userSession != null;
    }

    /**
     * Send a message.
     *
     * @param message - message to be sent
     */
    protected void sendMessage(String message) {
        if (isSessionOpen())
            this.userSession.getAsyncRemote().sendText(message);
    }
}

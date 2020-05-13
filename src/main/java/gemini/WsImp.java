package gemini;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.websocket.*;
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
            if (System.currentTimeMillis() - startTime > 10000) {
                LOGGER.fine("Heartbeat thread reconnecting.");
                this.ws.connect();
                this.interrupt();
            }
        }
    }
}

@ClientEndpoint
public class WsImp {

    private final static Logger LOGGER = Logger.getLogger(coinbase.WsImp.class.getName());
    private final static String URL = "wss://api.gemini.com/v1/marketdata/";
    private final static String QUERY = "?heartbeat=true&trades=true";
    private static final int MAX_LATENCY = 15000;
    private final static int RETRY_PERIOD = 3000;

    private WebSocketContainer container;
    private HeartbeatThread heartbeatThread;
    private Session userSession;
    private String symbol;
    // last price of ticker
    private float lastPrice;
    // last sequence number of ticker
    private long seqNum;

    /**
     * Gemini web socket client implementation for one symbol
     */
    public WsImp(String symbol) {
        this.container = ContainerProvider.getWebSocketContainer();
        this.heartbeatThread = null;
        this.lastPrice = -1f;
        this.seqNum = -1L;
        this.symbol = symbol;
        this.connect();
        this.waitForData();
    }

    /**
     * Connects to Gemini web socket server
     */
    void connect() {
        try {
            this.container.connectToServer(this, URI.create(URL + this.symbol + QUERY));
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
        LOGGER.info(String.format("Connected to: %s", URL + this.symbol + QUERY));
        this.userSession = userSession;
        this.heartbeatThread = new HeartbeatThread(this);
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
        JsonObject response = JsonParser.parseString(message).getAsJsonObject();
        String type = response.get("type").getAsString();

        if (type.equalsIgnoreCase("update") && response.get("events").getAsJsonArray().size() > 0) {
            // checks latency on update
            check_latency(response.get("timestampms").getAsLong());
            //checks seqNum
            long newSeqNum = response.get("socket_sequence").getAsLong();
            // if this update is more recent than the one we have in memory
            if (newSeqNum > this.seqNum) {
                this.seqNum = newSeqNum;
                JsonArray data = response.get("events").getAsJsonArray();
                JsonObject lastOfArr = data.get(data.size() - 1).getAsJsonObject();
                if (lastOfArr.get("type").getAsString().equals("trade"))
                    new Thread(() -> update_ticker(lastOfArr)).start();
            }
        }
    }


    /**
     * Updates memory data when receives "trade" event
     *
     * @param data received
     */
    private void update_ticker(JsonObject data) {
        this.lastPrice = data.get("price").getAsFloat();
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
        LOGGER.fine("Waiting for data.");
        while (this.lastPrice < 0.0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
        LOGGER.fine("Data received.");
    }

    /**
     * Checks latency on a websocket update
     *
     * @param updateTime
     */
    private void check_latency(long updateTime) {
        long latency = System.currentTimeMillis() - updateTime;
        if (latency > MAX_LATENCY) {
            if (!this.heartbeatThread.isInterrupted())
                this.heartbeatThread.interrupt();
            this.heartbeatThread = null;
            LOGGER.warning(String.format("Reconnecting to websocket due to high latency of: %d", latency));
            this.userSession = null;
            this.connect();
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
    }

    /**
     * Send a message.
     *
     * @param message - message to be sent
     */
    protected void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }
}

package bitstamp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

@ClientEndpoint
public class WsImp {

    private final static Logger LOGGER = Logger.getLogger(WsImp.class.getName());
    private final static String URL = "wss://ws.bitstamp.net";
    private final static int RETRY_PERIOD = 3000;
    private final static int MAX_LATENCY = 15000;

    private final WebSocketContainer container;
    private Session userSession;
    private final String symbol;
    private float lastPrice;

    /**
     * Bitstamp web socket client implementation for one symbol
     */
    public WsImp(String symbol) {
        this.container = ContainerProvider.getWebSocketContainer();
        this.lastPrice = -1f;
        this.symbol = symbol;
        this.connect();
        this.waitForData();
    }

    /**
     * Connects to BitStamp web socket server
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
        this.sendMessage(String.format("{\"event\": \"bts:subscribe\",\"data\": {\"channel\": \"live_trades_%s\"}}", symbol));
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param reason the reason for connection close
     */
    @OnClose
    public void onClose(CloseReason reason) {
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
        JsonObject response = JsonParser.parseString(message).getAsJsonObject();
        String event = response.get("event").getAsString();
        JsonObject data = response.get("data").getAsJsonObject();

        if (event.equalsIgnoreCase("trade")) {
            LOGGER.fine("Received trade data.");
            new Thread(() -> update_ticker(data)).start();
        } else if (event.equalsIgnoreCase("bts:error"))
            LOGGER.warning(response.get("data").getAsString());
        else if (event.equalsIgnoreCase("bts:request_reconnect"))
            this.connect();
    }


    /**
     * Updates memory data when receives "trade" event
     *
     * @param data received
     */
    private void update_ticker(JsonObject data) {
        check_latency(data.get("microtimestamp").getAsLong() / 1000);
        lastPrice = data.get("price_str").getAsFloat();
    }

    /**
     * Checks latency on a websocket update
     * @param timestamp - epoch stamp in ms
     */
    private void check_latency(long timestamp) {
        long latency = System.currentTimeMillis() - timestamp;
        if( latency > MAX_LATENCY) {
            LOGGER.warning(String.format("Reconnecting to websocket due to high latency of: %d", latency));
            this.closeSession();
            this.connect();
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
    private void waitForData()  {
        while (this.lastPrice < 0.0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Do nothing
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
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // Do nothing
        }
        this.closeSession();
        this.connect();
    }

    /**
     * Gets current status of websocket connection
     * @return true if websocket connection is open
     */
    public boolean isSessionOpen() {
        return this.userSession != null;
    }

    /**
     * Closes current websocket session
     */
    public void closeSession() {
        try {
            this.userSession.close();
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
        this.userSession = null;
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
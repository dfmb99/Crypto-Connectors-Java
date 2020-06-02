package kraken;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
        boolean sentPing = false;
        while (!Thread.interrupted()) {
            if (System.currentTimeMillis() - startTime > 5000 && !sentPing) {
                sentPing = true;
                LOGGER.finest("Heartbeat thread sending ping.");
                this.ws.sendMessage("{\"event\": \"ping\"}");
            } else if (System.currentTimeMillis() - startTime > 10000) {
                LOGGER.finest("Heartbeat thread reconnecting.");
                this.ws.closeSession();
                this.interrupt();
            }
        }
    }
}

@ClientEndpoint
public class WsImp {

    private final static Logger LOGGER = Logger.getLogger(WsImp.class.getName());
    private final static String URL = "wss://ws.kraken.com";
    private final static int RETRY_PERIOD = 3000;

    private final WebSocketContainer container;
    private HeartbeatThread heartbeatThread;
    private Session userSession;
    private final String symbol;
    // last price of ticker
    private float lastPrice;
    // subscription channelID
    private int tickerID;

    /**
     * Kraken web socket client implementation for one symbol
     */
    public WsImp(String symbol) {
        this.container = ContainerProvider.getWebSocketContainer();
        this.heartbeatThread = null;
        this.lastPrice = -1f;
        this.tickerID = -1;
        this.symbol = symbol;
        this.connect();
        this.waitForData();
    }

    /**
     * Connects to Kraken web socket server
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
        this.sendMessage(String.format("{\"event\": \"subscribe\",\"pair\": [\"%s\"],\"subscription\": {\"name\": \"ticker\"}}", this.symbol));
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

        // if message starts with "{" we can parse it to json otherwise we received an array of data
        if(message.startsWith("{")) {
            JsonObject response = JsonParser.parseString(message).getAsJsonObject();
            if(response.has("channelName") && response.has("status") && response.get("channelName").getAsString().equalsIgnoreCase("ticker") && response.get("status").getAsString().equalsIgnoreCase("subscribed"))
                this.tickerID = response.get("channelID").getAsInt();
            else if(response.has("event") && response.get("event").getAsString().equalsIgnoreCase("error"))
                LOGGER.warning(response.get("errorMessage").getAsString());
        } else {
            LOGGER.fine("Received ticker data.");
            JsonArray dataArr = JsonParser.parseString(message).getAsJsonArray();
            if(dataArr.get(0).getAsInt() == this.tickerID)
                new Thread(() -> update_ticker(dataArr)).start();
        }
    }


    /**
     * Updates memory data when receives "ticker" event
     *
     * @param data received
     */
    private void update_ticker(JsonArray data) {
        this.lastPrice = Float.parseFloat(data.get(1).getAsJsonObject().get("c").getAsJsonArray().get(0).getAsString());
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
     * @return true if session closed with success, false otherwise
     */
    public boolean closeSession() {
        if(isSessionOpen()) {
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
        if(isSessionOpen())
            this.userSession.getAsyncRemote().sendText(message);
    }
}

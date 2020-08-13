package binance.ws;

import binance.data.WsData;
import binance.rest.RestImp;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ClientEndpoint
public class WsImp implements Ws{
    private static final Logger logger = LogManager.getLogger(WsImp.class.getName());
    private final WebSocketContainer container;
    private final RestImp rest;
    private final Gson g;
    private Session userSession;
    private final String url;
    private final String subscriptions;
    private final String apiKey;
    private final String apiSecret;
    private final String symbol;
    // minimum timestamp to check latency on instrument update
    private long minReconnectTimeStamp;
    // wait / notification mechanism to wait for updates before allowing methods to be executed
    private final Object wsDataUpdate = "Web socket data update";
    // data structure to store ws data
    private final Map<String, Object> data;

    /**
     * Binance web socket client implementation for one symbol
     *
     * @param rest             - binance rest api object
     * @param url              - true if we want to connect to testnet, false otherwise
     * @param apiKey           - apiKey
     * @param apiSecret        - apiSecret
     * @param symbol           - symbol to subscribe
     */
    public WsImp(RestImp rest, String url, String apiKey, String apiSecret, String symbol) throws InterruptedException {
        this.container = ContainerProvider.getWebSocketContainer();
        this.g = new Gson();
        this.rest = rest;
        this.url = url;
        this.subscriptions = String.format("/stream?streams=%s@aggTrade/%s@markPrice/%s@kline_1m/%s@miniTicker/%s@forceOrder", symbol, symbol, symbol, symbol, symbol);
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.userSession = null;
        this.data = new ConcurrentHashMap<>();
        this.minReconnectTimeStamp = 0L;
        this.symbol = symbol;

        this.connect();
        this.waitForData();
    }

    /**
     * Connects to Binance web socket server
     */
    void connect() {
        try {
            this.container.connectToServer(this, URI.create(this.url + this.subscriptions));
        } catch (Exception e) {
            e.printStackTrace();
            ThreadContext.put("ROUTINGKEY", symbol);
            logger.error("Failed to connect to web socket server.");
            try {
                Thread.sleep(RETRY_PERIOD);
            } catch (InterruptedException e2) {
                //Nothing to be done here, if this happens we will just retry sooner.
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
        ThreadContext.put("ROUTINGKEY", symbol);
        logger.info(String.format("Connected to: %s", this.url));
        this.userSession = userSession;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param reason the reason for connection close
     */
    @OnClose
    public void onClose(CloseReason reason) {
        ThreadContext.put("ROUTINGKEY", symbol);
        logger.info(String.format("Websocket closed with code: %d", reason.getCloseCode().getCode()));
        this.userSession = null;
        this.connect();
    }

    /**
     * Callback hook for Error Events. This method will be invoked when a client receives a error.
     *
     * @param throwable - Error thrown
     */
    @OnError
    public void onError(Throwable throwable) {
        ThreadContext.put("ROUTINGKEY", symbol);
        logger.error("Websocket error: ", throwable);
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        ThreadContext.put("ROUTINGKEY", symbol);
        if (!isSessionOpen())
            return;
        if(message.equalsIgnoreCase("ping")) {
            sendMessage("pong");
            return;
        }

        WsData dataRec = g.fromJson(message, WsData.class);
        switch (dataRec.getStream()) {
            case MINI_TICKER:
                new Thread(() -> update_miniTicker(dataRec.getData()) );
                break;
            case KLINE_1M:
                new Thread(() -> update_kline(dataRec.getData()) );
                break;
            case AGG_TRADE:
                new Thread(() -> update_aggTrade(dataRec.getData()) );
                break;
            case MARK_PRICE:
                new Thread(() -> update_markPrice(dataRec.getData()) );
                break;
            case LIQUIDATION:
                new Thread(() -> update_liquidation(dataRec.getData()) );
                break;
        }
        logger.debug(message);
    }

    private void update_liquidation(JsonObject data) {
    }

    private void update_markPrice(JsonObject data) {
    }

    private void update_aggTrade(JsonObject data) {
    }

    private void update_kline(JsonObject data) {
    }

    private void update_miniTicker(JsonObject data) {

    }

    @Override
    public boolean isSessionOpen() {
        return this.userSession != null;
    }

    @Override
    public boolean closeSession() {
        if (isSessionOpen()) {
            try {
                this.userSession.close();
                return true;
            } catch (IOException e) {
                ThreadContext.put("ROUTINGKEY", symbol);
                logger.error("Could not close user session.");
                return false;
            }
        }
        return false;
    }

    /**
     * waits for instrument ws data, blocking thread
     */
    private void waitForData() throws InterruptedException {
        synchronized (wsDataUpdate) {
            wsDataUpdate.wait();
        }
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

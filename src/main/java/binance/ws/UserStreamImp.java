package binance.ws;

import binance.rest.RestImp;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.websocket.*;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static binance.ws.UserStream.RETRY_PERIOD;

@ClientEndpoint
public class UserStreamImp {
    private static final Logger logger = LogManager.getLogger(UserStream.class.getName());
    private final WebSocketContainer container;
    private final Gson g;
    private final RestImp rest;
    private Session userSession;
    private final String url;
    private final String symbol;
    private final String listenKey;
    // minimum timestamp to check latency on instrument update
    private long minReconnectTimeStamp;
    // check latency sync lock
    private final Object latencyLock = "Latency checking lock";
    // wait / notification mechanism to wait for updates before allowing methods to be executed
    private final Object wsDataUpdate = "Web socket data update";
    // data structure to store ws data
    private final Map<String, Object> wsData;
    /**
     * Binance web socket client implementation
     *
     * @param url              - true if we want to connect to testnet, false otherwise
     * @param symbol           - symbol to subscribe
     */
    public UserStreamImp(RestImp rest, String url, String symbol) throws InterruptedException {
        this.container = ContainerProvider.getWebSocketContainer();
        this.rest = rest;
        this.g = new Gson();
        this.url = url;
        this.userSession = null;
        this.wsData = new ConcurrentHashMap<>();
        this.minReconnectTimeStamp = 0L;
        this.symbol = symbol;
        this.listenKey = rest.start_user_stream().getListenKey();

        this.connect();
        this.waitForData();
    }

    /**
     * Connects to Binance web socket server
     */
    void connect() {
        try {
            this.container.connectToServer(this, URI.create(this.url + "/ws/" + this.listenKey));
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
    }

    /**
     * waits for instrument ws data, blocking thread
     */
    private void waitForData() throws InterruptedException {
        synchronized (wsDataUpdate) {
            wsDataUpdate.wait();
        }
    }
}

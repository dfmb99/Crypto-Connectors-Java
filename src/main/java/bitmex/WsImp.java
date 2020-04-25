package bitmex;

import bitmex.utils.Auth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

@ClientEndpoint
public class WsImp implements Ws {

    private final static Logger LOGGER = Logger.getLogger(Bitmex.class.getName());
    private WebSocketContainer container;
    private Session userSession;
    private String url;
    private String apiKey;
    private String apiSecret;
    private String symbol;
    private String subscriptions;
    private ConcurrentMap<String, JsonArray> data;

    /**
     * Bitmex WebSocket client implementation
     *
     * @param url - ws endpoint to connect
     */
    public WsImp(String url, String apiKey, String apiSecret, String symbol) {
        this.container = ContainerProvider.getWebSocketContainer();
        this.url = url;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.symbol = symbol;
        this.userSession = null;
        this.subscriptions = "";
        this.data = new ConcurrentHashMap<String, JsonArray>();
    }

    /**
     * Sets subscriptions to be sent to the ws server
     *
     * @param sub - subscriptions as String
     */
    public void setSubscriptions(String sub) {
        this.subscriptions = sub;
        String[] split = sub.split(",");
        for(String str: split) {
            this.data.put(str.split(":")[0], JsonParser.parseString("[{}]").getAsJsonArray());
        }
    }

    /**
     * Initialize webSocket connection
     */
    public void initConnection() {
        this.connect();
    }

    /**
     * Returns true if ws connection is opened
     *
     * @return true if open, false otherwise
     */
    public boolean getSessionStatus() {
        return this.userSession != null;
    }

    /**
     * Closes this current user session
     */
    public void closeConnection() {
        try {
            this.userSession.close();
        } catch (IOException e) {
            LOGGER.warning("Could not close ws connection.");
        }
    }

    private void connect() {
        try {
            this.container.connectToServer(this, URI.create(this.url));
        } catch (Exception e) {
            try {
                LOGGER.warning("Failed to connect to websocket server.");
                Thread.sleep(Ws.RETRY_PERIOD); //wait until attempting again.
            } catch (InterruptedException ie) {
                //Nothing to be done here, if this happens we will just retry sooner.
            }
        }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
        long expires = Auth.generate_expires();
        String signature = Auth.encode_hmac(apiSecret, String.format("%s%d", "GET/realtime", expires));
        sendMessage(String.format("{\"op\": \"authKeyExpires\", \"args\": [\"%s\", %d, \"%s\"]}", apiKey, expires, signature));
        sendMessage(String.format("{\"op\": \"subscribe\", \"args\": [\"%s\"]}",
                this.subscriptions));
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param reason the reason for connection close
     */
    @OnClose
    public void onClose(CloseReason reason) {
        LOGGER.warning(String.format("Websocket closed with code: %d \n Message: %s", reason.getCloseCode(), reason.getReasonPhrase()));
        this.userSession = null;
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        LOGGER.fine(message);
        JsonObject obj = JsonParser.parseString(message).getAsJsonObject();
        if (obj.has("subscribe")) {
            LOGGER.info("Subscribed successfully to: " + obj.get("subscribe"));
        } else if (obj.has("status")) {
            LOGGER.warning(obj.get("error").getAsString());
        } else if (obj.has("table")) {
            if (obj.get("table").getAsString().equals("instrument"))
                update_intrument(obj);
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'instrument'
     *
     * @param obj - obj received from ws
     */
    private void update_intrument(JsonObject obj) {
        JsonObject instrumentData = data.get("instrument").get(0).getAsJsonObject();
        if (obj.get("action").getAsString().equals("update")) {
            Set<Map.Entry<String, JsonElement>> entry = obj.get("data").getAsJsonArray().get(0).getAsJsonObject().entrySet();
            Iterator<Map.Entry<String, JsonElement>> it = entry.iterator();
            Map.Entry<String, JsonElement> next;
            while (it.hasNext()) {
                next = it.next();
                instrumentData.addProperty(next.getKey(), next.getValue().getAsString());
            }
        }
    }

    /**
     * Callback hook for Error Events. This method will be invoked when a client receives a error.
     *
     * @param userSession
     * @param throwable
     */
    @OnError
    public void onError(Session userSession, Throwable throwable) {
        LOGGER.warning(throwable.getMessage());
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

}

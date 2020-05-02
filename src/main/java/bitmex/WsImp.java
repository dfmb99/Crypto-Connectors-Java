package bitmex;

import bitmex.utils.Auth;
import bitmex.utils.BinarySearch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**class MyThread extends Thread {
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            System.out.println("I am running....");
        }

        System.out.println("Stopped Running.....");
    }
}*/

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
        String[] split = sub.replaceAll("\"", "").split(",");
        for (String str : split) {
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
        sendMessage(String.format("{\"op\": \"subscribe\", \"args\": [%s]}",
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
            LOGGER.info("Subscribed successfully to " + obj.get("subscribe"));
        } else if (obj.has("status")) {
            LOGGER.warning(obj.get("error").getAsString());
        } else if (obj.has("table")) {
            if (obj.get("table").getAsString().equals("instrument")) {
                update_intrument(obj);
            } else if (obj.get("table").getAsString().equals("orderBookL2")) {
                update_orderBookL2(obj);
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'instrument'
     *
     * @param obj - obj received from ws
     */
    private void update_intrument(JsonObject obj) {
        JsonObject instrumentData = this.data.get("instrument").get(0).getAsJsonObject();
        if (obj.get("action").getAsString().equals("update")) {
            JsonObject data = obj.get("data").getAsJsonArray().get(0).getAsJsonObject();
            for (String key : data.keySet()) {
                instrumentData.addProperty(key, data.get(key).getAsString());
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'orderBookL2'
     *
     * @param obj - obj received from ws
     */
    private void update_orderBookL2(JsonObject obj) {
        JsonArray orderbookData = this.data.get("orderBookL2");
        JsonArray data = obj.get("data").getAsJsonArray();
        if (obj.get("action").getAsString().equals("partial")) {
            this.data.put("orderBookL2", data);
        } else {
            //checks every row on data array
            for (JsonElement elem : data) {
                long id = elem.getAsJsonObject().get("id").getAsLong();
                long[] ids = new long[orderbookData.size()];
                for (int i = 0; i < orderbookData.size(); i++)
                    ids[i] = orderbookData.get(i).getAsJsonObject().get("id").getAsLong();

                int index = BinarySearch.binarySearchL(ids, 0, ids.length - 1, id);
                if (index == -1) {
                    orderbookData = insert(BinarySearch.getIndexInSortedArray(ids, ids.length - 1, id), elem,
                            orderbookData);
                } else {
                    JsonObject bookline = orderbookData.get(index).getAsJsonObject();
                    if (obj.get("action").getAsString().equals("update"))
                        bookline.addProperty("size", elem.getAsJsonObject().get("size").getAsLong());
                    else if (obj.get("action").getAsString().equals("delete"))
                        bookline.addProperty("size", 0L);
                    bookline.addProperty("side", elem.getAsJsonObject().get("side").getAsString());
                }
            }
        }
    }

    /**
     * Gets size of level2 orderbook row with price == 'price'
     *
     * @param price - price of row to query
     * @return size - size of orderbook row
     */
    protected long getL2Size(float price) {
        JsonArray orderbookData = this.data.get("orderBookL2");
        float[] ids = new float[orderbookData.size()];
        for (int i = 0; i < orderbookData.size(); i++)
            ids[i] = orderbookData.get(i).getAsJsonObject().get("price").getAsFloat();

        int index = BinarySearch.binarySearchF(ids, 0, ids.length - 1, price);
        return orderbookData.get(index).getAsJsonObject().get("size").getAsLong();
    }

    /**
     * Adds a JsonElement to a JSonArray in a given index
     *
     * @param index        - index on JsonArray to add element
     * @param val          - element to be added
     * @param currentArray - JsonArray to be processed
     * @return
     */
    private JsonArray insert(int index, JsonElement val, JsonArray currentArray) {
        JsonArray newArray = new JsonArray();
        for (int i = 0; i < index; i++) {
            newArray.add(currentArray.get(i));
        }
        newArray.add(val);

        for (int i = index; i < currentArray.size(); i++) {
            newArray.add(currentArray.get(i));
        }
        return newArray;
    }

    /**
     * Callback hook for Error Events. This method will be invoked when a client receives a error.
     *
     * @param userSession
     * @param throwable
     */
    @OnError
    public void onError(Session userSession, Throwable throwable) {
        LOGGER.warning(throwable.toString());
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

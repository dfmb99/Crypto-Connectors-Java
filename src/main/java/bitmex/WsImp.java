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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

/**
 * Heartbeat thread that sends ping messages to the websocket server
 */
class Heartbeat extends Thread {
    private WsImp ws;
    private final static Logger LOGGER = Logger.getLogger(Heartbeat.class.getName());

    public Heartbeat(WsImp ws) {
        this.ws = ws;
        this.start();
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(5000);
                LOGGER.fine("Heartbeat thread sending ping.");
                this.ws.sendMessage("ping");
                Thread.sleep(5000);
                LOGGER.finest("Heartbeat thread reconnecting.");
                this.ws.closeConnection();
                this.ws.initConnection();
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }
}

/**
 * Thread that deals with web socket messages with table = "order"
 */
class OrderUpdate extends Thread {
    private WsImp ws;
    private final Deque<JsonObject> queue;
    private final static Logger LOGGER = Logger.getLogger(OrderUpdate.class.getName());

    public OrderUpdate(WsImp ws) {
        this.ws = ws;
        this.queue = new ConcurrentLinkedDeque<>();
        this.start();
    }

    public void add( JsonObject obj){
        this.queue.addLast(obj); // blocks until there is free space in the optionally bounded queue
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            JsonObject element;
            while ((element = queue.poll()) != null) { // does not block on empty list but returns null instead
                this.ws.update_order(element);
            }
        }
    }
}

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
    // order messages from web socket need to be ordered and processed synchronously
    private OrderUpdate orderQueue;
    //structure to store web socket data in local storage
    private Map<String, JsonArray> data;
    private Heartbeat heartbeat;

    /**
     * BitMex web socket client implementation
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
        this.heartbeat = null;
        this.data = new ConcurrentHashMap<>();
    }

    /**
     * Sets subscriptions to be sent to the ws server
     *
     * @param sub - subscriptions as String
     */
    public void setSubscriptions(String sub) {
        this.subscriptions = sub;
        String[] split = sub.split(",");
        for (String str : split) {
            String s = str.split(":")[0];
            this.data.put(s, JsonParser.parseString("[]").getAsJsonArray());
        }

        this.data.get("instrument").add(JsonParser.parseString("{}").getAsJsonObject());
        this.orderQueue = new OrderUpdate(this);
    }

    /**
     * Initialize webSocket connection, if there is no user session
     */
    public void initConnection() {
        if (!this.getSessionStatus())
            this.connect();
    }

    /**
     * Returns true if web socket connection is opened
     *
     * @return true if open,
     * false otherwise
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

    /**
     * Connects to BitMex web socket server
     */
    private void connect() {
        try {
            this.container.connectToServer(this, URI.create(this.url));
        } catch (Exception e) {
            try {
                LOGGER.warning("Failed to connect to web socket server.");
                Thread.sleep(Ws.RETRY_PERIOD); //wait until attempting again.
                this.connect();
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
        this.heartbeat = new Heartbeat(this);
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
        if (!this.heartbeat.isInterrupted())
            this.heartbeat.interrupt();
        this.heartbeat = null;
        LOGGER.warning(String.format("Websocket closed with code: %d \n Message: %s", reason.getCloseCode().getCode(),
                reason.getReasonPhrase()));
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
        if (!this.heartbeat.isInterrupted())
            this.heartbeat.interrupt();
        this.heartbeat = new Heartbeat(this);
        LOGGER.fine(message);
        //if it was an heartbeat message
        if (message.equalsIgnoreCase("pong"))
            return;
        JsonObject obj = JsonParser.parseString(message).getAsJsonObject();
        if (obj.has("subscribe")) {
            LOGGER.info("Subscribed successfully to " + obj.get("subscribe"));
        } else if (obj.has("status")) {
            LOGGER.warning(obj.get("error").getAsString());
        } else if (obj.has("table")) {
            String table = obj.get("table").getAsString();
            switch (table) {
                case "instrument":
                    new Thread(()-> { update_intrument(obj); }).start();
                    break;
                case "orderBookL2":
                    new Thread(()-> { update_orderBookL2(obj); }).start();
                    break;
                case "liquidation":
                    new Thread(()-> { update_liquidation(obj); }).start();
                    break;
                case "order":
                    // adds message to the queue that leads with order messages;
                    orderQueue.add(obj);
                    break;
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'instrument'
     *
     * @param obj - obj received from ws
     */
    private void update_intrument(JsonObject obj) {
        if (obj.get("action").getAsString().equals("update")) {
            JsonObject instrumentData = this.data.get("instrument").get(0).getAsJsonObject();
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
        JsonArray data = obj.get("data").getAsJsonArray();
        if (obj.get("action").getAsString().equals("partial")) {
            this.data.put("orderBookL2", data);
        } else {
            JsonArray orderbookData = this.data.get("orderBookL2");
            if (orderbookData == null)
                return;
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
     * Updates data in memory after receiving an ws message with table = 'liquidation'
     *
     * @param obj - obj received from ws
     */
    private void update_liquidation(JsonObject obj) {
        JsonArray liquidationData = this.data.get("liquidation");
        JsonArray data = obj.get("data").getAsJsonArray();
        if (obj.get("action").getAsString().equals("insert")) {
            for (JsonElement elem : data) {
                synchronized (liquidationData) {
                    if (liquidationData.size() == Ws.MAX_TABLE_LEN)
                        liquidationData.remove(0);
                    liquidationData.add(elem);
                }
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'order'
     *
     * @param obj - obj received from ws
     */
    protected void update_order(JsonObject obj) {
        this.data.putIfAbsent("order", JsonParser.parseString("[]").getAsJsonArray());
        JsonArray orderData = this.data.get("order");
        JsonArray data = obj.get("data").getAsJsonArray();
        if (obj.get("action").getAsString().equals("insert")) {
            for (JsonElement elem : data) {
                orderData.add(elem);
            }
        } else if (obj.get("action").getAsString().equals("update")) {
            for (JsonElement elem : data) {
                //orderData.add(elem);
            }
        }
    }

    /**
     * Gets size of level2 orderBook row with price == 'price'
     *
     * @param price - price of row to query
     * @return size - size of orderBook row if data is available
     * -1 otherwise
     */
    protected long getL2Size(float price) {
        if (this.data.get("orderBookL2") == null)
            return -1L;
        JsonArray orderbookData = this.data.get("orderBookL2");
        float[] ids = new float[orderbookData.size()];
        for (int i = 0; i < orderbookData.size(); i++)
            ids[i] = orderbookData.get(i).getAsJsonObject().get("price").getAsFloat();

        int index = BinarySearch.binarySearchF(ids, 0, ids.length - 1, price);
        return orderbookData.get(index).getAsJsonObject().get("size").getAsLong();
    }

    protected void checkDelay(JsonObject obj) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        System.out.print(formatter.format(date) + ": ");
        System.out.println(obj.get("timestamp"));
    }

    /**
     * Adds a JsonElement to a JSonArray in a given index
     *
     * @param index        - index on JsonArray to add element
     * @param val          - element to be added
     * @param currentArray - JsonArray to be processed
     * @return newArray - new JsonArray
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
     * @param userSession - current user session
     * @param throwable   - Error thrown
     */
    @OnError
    public void onError(Session userSession, Throwable throwable) {
        LOGGER.warning(throwable.toString());
    }

    /**
     * Send a message.
     *
     * @param message - message to be sent
     */
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

}

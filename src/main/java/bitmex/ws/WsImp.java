package bitmex.ws;

import bitmex.rest.Rest;
import bitmex.rest.RestImp;
import utils.Auth;
import utils.BinarySearch;
import com.google.gson.*;
import utils.TimeStamp;

import javax.websocket.*;
import java.net.URI;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
                LOGGER.fine("Heartbeat thread sending ping.");
                this.ws.sendMessage("ping");
            } else if (System.currentTimeMillis() - startTime > 10000) {
                LOGGER.fine("Heartbeat thread reconnecting.");
                this.ws.connect();
                this.interrupt();
            }
        }
    }
}

/**
 * Thread that deals with web socket messages with table = "order"
 */
class OrderAsyncThread extends Thread {
    private final WsImp ws;
    private final Deque<JsonObject> queue;
    private final static Logger LOGGER = Logger.getLogger(OrderAsyncThread.class.getName());

    public OrderAsyncThread(WsImp ws) {
        this.ws = ws;
        this.queue = new ConcurrentLinkedDeque<>();
        this.start();
    }

    public void add(JsonObject obj) {
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

    private final static Logger LOGGER = Logger.getLogger(Rest.class.getName());
    private WebSocketContainer container;
    private RestImp rest;
    private Session userSession;
    private final String url;
    private final String apiKey;
    private final String apiSecret;
    private String subscriptions;
    private String symbol;
    // order messages from web socket need to be ordered and processed synchronously
    private OrderAsyncThread orderQueue;
    // structure to store web socket data in local storage
    private final Map<String, JsonArray> data;
    private HeartbeatThread heartbeatThread;
    private final Gson g;

    /**
     * BitMex web socket client implementation for one symbol
     *
     * @param rest - bitmex rest api object
     * @param testnet - true if we want to connect to testnet, false otherwise
     * @param apiKey - apiKey
     * @param apiSecret - apiSecret
     * @param symbol - symbol to subscribe
     */
    public WsImp(RestImp rest, boolean testnet, String apiKey, String apiSecret, String symbol) {
        this.container = ContainerProvider.getWebSocketContainer();
        this.rest = rest;
        if (testnet)
            this.url = Ws.WS_TESTNET;
        else
            this.url = Ws.WS_MAINNET;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.userSession = null;
        this.heartbeatThread = null;
        this.data = new ConcurrentHashMap<>();
        this.g = new Gson();
        this.symbol = symbol;
        this.setSubscriptions("\"instrument:"+ symbol +"\",\"orderBookL2:"+ symbol +"\",\"liquidation:"+ symbol +"\"," +
                "\"order:"+ symbol +"\",\"position:"+ symbol +"\",\"execution:"+ symbol +"\",\"tradeBin1m:"+ symbol +"\",\"margin:*\"");
        this.connect();
        this.waitForData();
    }

    /**
     * Sets subscriptions to be sent to the ws server
     *
     * @param sub - subscriptions as String
     */
    private void setSubscriptions(String sub) {
        this.subscriptions = sub;
        String[] split = sub.split(",");
        for (String str : split) {
            String[] strArr = str.split(":");
            this.data.put(strArr[0].substring(1), new JsonArray());
        }

        if (this.data.containsKey("instrument"))
            this.data.get("instrument").add(new JsonObject());
        if (this.data.containsKey("margin"))
            this.data.get("margin").add(new JsonObject());
        if (this.data.containsKey("position"))
            this.data.get("position").add(new JsonObject());
        if (this.data.containsKey("order"))
            this.orderQueue = new OrderAsyncThread(this);
        if(this.data.containsKey("tradeBin1m") && this.rest != null)
            this.data.put("tradeBin1m", this.get_last_1mCandles());
    }

    /**
     * Connects to BitMex web socket server
     */
    void connect() {
        try {
            this.container.connectToServer(this, URI.create(this.url));
        } catch (Exception e) {
            LOGGER.warning("Failed to connect to web socket server.");
            try {
                Thread.sleep(Ws.RETRY_PERIOD);
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
        LOGGER.info(String.format("Connected to: %s", this.url));
        this.heartbeatThread = new HeartbeatThread(this);
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
    public void onMessage(String message) throws InterruptedException {
        if (!this.heartbeatThread.isInterrupted())
            this.heartbeatThread.interrupt();
        this.heartbeatThread = new HeartbeatThread(this);
        //if it was an heartbeat message
        if (message.equalsIgnoreCase("pong"))
            return;
        JsonObject obj = JsonParser.parseString(message).getAsJsonObject();
        if (obj.has("subscribe")) {
            LOGGER.fine("Subscribed successfully to " + obj.get("subscribe"));
        } else if (obj.has("status")) {
            LOGGER.warning(obj.get("error").getAsString());
            // Rate limited
            if (obj.get("status").getAsInt() == 429) {
                long waitTime = obj.get("meta").getAsJsonObject().get("retryAfter").getAsLong();
                LOGGER.warning(String.format("Rate-limited, retrying on %d seconds.", waitTime));
                Thread.sleep(waitTime * 1000);
            } else
                System.exit(1);
        } else if (obj.has("table")) {
            String table = obj.get("table").getAsString();
            switch (table) {
                case "instrument":
                    new Thread(() -> update_intrument(obj)).start();
                    break;
                case "orderBookL2":
                    new Thread(() -> update_orderBookL2(obj)).start();
                    break;
                case "liquidation":
                    new Thread(() -> update_liquidation(obj)).start();
                    break;
                case "margin":
                    new Thread(() -> update_margin(obj)).start();
                    break;
                case "position":
                    new Thread(() -> update_position(obj)).start();
                    break;
                case "tradeBin1m":
                    new Thread(() -> update_tradeBin1m(obj)).start();
                    break;
                case "execution":
                    new Thread(() -> update_execution(obj)).start();
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
     * @param obj - obj received from web socket
     */
    private void update_intrument(JsonObject obj) {
        String action = obj.get("action").getAsString();
        if (action.equals("partial")) {
            this.data.put("instrument", obj.get("data").getAsJsonArray());
        } else if (action.equals("update")) {
            JsonObject instrumentData = this.data.get("instrument").get(0).getAsJsonObject();
            JsonObject data = obj.get("data").getAsJsonArray().get(0).getAsJsonObject();
            // checks latency on the update
            check_latency(data.get("timestamp").getAsString());
            for (String key : data.keySet()) {
                instrumentData.addProperty(key, data.get(key).getAsString());
            }
        }
    }

    /**
     * Checks latency on a websocket update
     * @param timestamp
     */
    private void check_latency(String timestamp) {
        long updateTime = TimeStamp.getTimestamp(timestamp);
        long latency = System.currentTimeMillis() - updateTime;
        if( latency > Ws.MAX_LATENCY) {
            if (!this.heartbeatThread.isInterrupted())
                this.heartbeatThread.interrupt();
            this.heartbeatThread = null;
            LOGGER.warning(String.format("Reconnecting to websocket due to high latency of: %d", latency));
            this.userSession = null;
            this.connect();
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
        String action = obj.get("action").getAsString();
        JsonArray liquidationData = this.data.get("liquidation");
        JsonArray data = obj.get("data").getAsJsonArray();
        if (action.equals("update") || action.equals("insert")) {
            for (JsonElement elem : data) {
                if (liquidationData.size() == Ws.LIQ_MAX_LEN)
                    liquidationData.remove(0);
                liquidationData.add(elem);
            }
        } else if (action.equals("delete")) {
            //copy of liquidationData to prevent ConcurrentModification Exception
            JsonArray liquidationDataCopy = JsonParser.parseString(liquidationData.toString()).getAsJsonArray();
            for (JsonElement elem : data) {
                // orderID in object received element
                String orderIDRec = elem.getAsJsonObject().get("orderID").getAsString();
                // iterates over liquidationData stored in memory/
                for (JsonElement elemOrig : liquidationDataCopy) {
                    // orderId in liquidationData element
                    String orderIDOrig = elemOrig.getAsJsonObject().get("orderID").getAsString();
                    // if same orderID
                    if (orderIDRec.equals(orderIDOrig)) {
                        liquidationData.remove(elemOrig);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'margin'
     *
     * @param obj - obj received from ws
     */
    private void update_margin(JsonObject obj) {
        String action = obj.get("action").getAsString();
        if (action.equals("update") || action.equals("partial")) {
            JsonObject marginData = this.data.get("margin").get(0).getAsJsonObject();
            JsonObject data = obj.get("data").getAsJsonArray().get(0).getAsJsonObject();
            for (String key : data.keySet()) {
                if (!data.get(key).isJsonNull())
                    marginData.addProperty(key, data.get(key).getAsString());
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'position'
     *
     * @param obj - obj received from ws
     */
    private void update_position(JsonObject obj) {
        String action = obj.get("action").getAsString();
        JsonArray dataArr = obj.get("data").getAsJsonArray();
        if (dataArr.size() > 0 && (action.equals("update") || action.equals("partial"))) {
            JsonObject positionData = this.data.get("position").get(0).getAsJsonObject();
            JsonObject data = dataArr.get(0).getAsJsonObject();
            for (String key : data.keySet()) {
                if (!data.get(key).isJsonNull())
                    positionData.addProperty(key, data.get(key).getAsString());
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'tradeBin1m'
     *
     * @param obj - obj received from ws
     */
    private void update_tradeBin1m(JsonObject obj) {
        String action = obj.get("action").getAsString();
        if (action.equals("insert")) {
            JsonArray tradeBin1mData = this.data.get("tradeBin1m");
            JsonArray data = obj.get("data").getAsJsonArray();
            for (JsonElement elem : data) {
                if (tradeBin1mData.size() == Ws.TRADE_BIN_MAX_LEN)
                    tradeBin1mData.remove(Ws.TRADE_BIN_MAX_LEN - 1);
                this.data.put("tradeBin1m", insert(0, elem, tradeBin1mData));
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'execution'
     *
     * @param obj - obj received from ws
     */
    private void update_execution(JsonObject obj) {
        String action = obj.get("action").getAsString();
        if (action.equals("insert") || action.equals("partial")) {
            JsonArray executionData = this.data.get("execution");
            JsonArray data = obj.get("data").getAsJsonArray();
            for (JsonElement elem : data) {
                if (executionData.size() == Ws.EXEC_MAX_LEN)
                    executionData.remove(0);
                executionData.add(elem);
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'order'
     *
     * @param obj - obj received from ws
     */
    protected void update_order(JsonObject obj) {
        JsonArray orderData = this.data.get("order");
        JsonArray data = obj.get("data").getAsJsonArray();
        if (obj.get("action").getAsString().equals("insert")) {
            for (JsonElement elem : data)
                orderData.add(elem);
        } else if (obj.get("action").getAsString().equals("update")) {
            //copy of orderData to prevent ConcurrentModification Exception
            JsonArray orderDataCopy = JsonParser.parseString(orderData.toString()).getAsJsonArray();
            boolean orderMatchFound;
            // iterates over object received
            for (JsonElement elemRec : data) {
                orderMatchFound = false;
                JsonObject objRec = elemRec.getAsJsonObject();
                // orderID in object received element
                String orderIDRec = objRec.get("orderID").getAsString();
                // ordStatus of order received
                JsonElement ordStatus = objRec.get("ordStatus");
                // iterates over orderData stored in memory/
                for (JsonElement elemOrig : orderDataCopy) {
                    // orderId in orderData element
                    String orderIDOrig = elemOrig.getAsJsonObject().get("orderID").getAsString();
                    // if same orderID
                    if (orderIDRec.equals(orderIDOrig)) {
                        orderMatchFound = true;
                        // if order still active we update, otherwise we delete the order
                        if (ordStatus == null || ordStatus.getAsString().equals("New") || ordStatus.getAsString().equals("PartiallyFilled")) {
                            // iterate data on object received and updates local memory
                            for (String key : objRec.keySet()) {
                                if (!objRec.get(key).isJsonNull())
                                    elemOrig.getAsJsonObject().addProperty(key, objRec.get(key).getAsString());
                            }
                        } else
                            orderData.remove(elemOrig);
                        break;
                    }
                }
                if (!orderMatchFound && (ordStatus == null || ordStatus.getAsString().equals("New") || ordStatus.getAsString().equals("PartiallyFilled")))
                    orderData.add(elemRec);
            }
        }
    }

    @Override
    public long getL2Size(float price) {
        if (this.data.get("orderBookL2") == null)
            return -1L;
        JsonArray orderbookData = this.data.get("orderBookL2");
        float[] ids = new float[orderbookData.size()];
        for (int i = 0; i < orderbookData.size(); i++)
            ids[i] = orderbookData.get(i).getAsJsonObject().get("price").getAsFloat();

        int index = BinarySearch.binarySearchF(ids, 0, ids.length - 1, price);
        return orderbookData.get(index).getAsJsonObject().get("size").getAsLong();
    }

    @Override
    public JsonArray get_open_liquidation() {
        return this.data.get("liquidations");
    }

    @Override
    public JsonArray get_instrument() {
        return this.data.get("instrument");
    }

    @Override
    public JsonArray get_trabeBin1m() {
        return this.data.get("tradeBin1m");
    }

    @Override
    public JsonArray get_orderBookL2() {
        return this.data.get("orderBookL2");
    }

    @Override
    public JsonArray get_margin() {
        return this.data.get("margin");
    }

    @Override
    public JsonArray get_execution() {
        return this.data.get("execution");
    }

    @Override
    public JsonArray get_position() {
        return this.data.get("position");
    }

    @Override
    public JsonArray get_openOrders(String orderIDPrefix) {
        JsonArray ret = new JsonArray();
        JsonArray openOrders = this.data.get("order");
        for (JsonElement elemRec : openOrders) {
            if(elemRec.getAsJsonObject().get("orderID").getAsString().startsWith(orderIDPrefix))
                ret.add(elemRec);
        }
        return ret;
    }

    /**
     * Makes api rest call to get last trade bucketed data
     * @return Returns last candles data for current symbol
     */
    public JsonArray get_last_1mCandles() {
        JsonObject params = new JsonObject();
        params.addProperty("binSize", "1m");
        params.addProperty("symbol", this.symbol);
        params.addProperty("count", Ws.TRADE_BIN_MAX_LEN);
        params.addProperty("reverse", true);
        return this.rest.get_trade_bucketed(params);
    }

    /**
     * waits for instrument ws data, blocking thread
     */
    private void waitForData() {
        LOGGER.fine("Waiting for data.");
        while( this.data.get("instrument").get(0).getAsJsonObject().get("lastPrice") == null ) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
        LOGGER.fine("Data received.");
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
     * @param throwable - Error thrown
     */
    @OnError
    public void onError(Throwable throwable) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // Do nothing
        }
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
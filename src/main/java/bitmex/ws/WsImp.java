package bitmex.ws;

import bitmex.data.*;
import bitmex.rest.RestImp;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import utils.Auth;
import utils.BinarySearch;
import utils.TimeStamp;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Heartbeat thread that sends ping messages to the websocket server
 */
class HeartbeatThread extends Thread {
    private final WsImp ws;

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
                this.ws.sendMessage("ping");
            } else if (System.currentTimeMillis() - startTime > 10000) {
                this.ws.closeSession();
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

    private static final Logger logger = LogManager.getLogger(WsImp.class.getName());
    private final WebSocketContainer container;
    private final RestImp rest;
    private final Gson g;
    private Session userSession;
    private final String url;
    private final String apiKey;
    private final String apiSecret;
    private final String subscriptions;
    private final String symbol;
    private final int tradeBinListSize;

    // order messages from web socket need to be ordered and processed synchronously
    private final OrderAsyncThread orderQueue;
    // data structure to store ws data
    private final Map<String, Object> data;
    // thread that deals with ws heartbeats
    private HeartbeatThread heartbeatThread;
    // minimum timestamp to check latency on instrument update
    private long minReconnectTimeStamp;
    // check latency sync lock
    private final Object latencyLock = "Latency checking lock";
    // wait / notification mechanism to wait for updates before allowing methods to be executed
    private final Object wsDataUpdate = "Web socket data update";

    /**
     * BitMex web socket client implementation for one symbol
     *
     * @param rest             - bitmex rest api object
     * @param testnet          - true if we want to connect to testnet, false otherwise
     * @param apiKey           - apiKey
     * @param apiSecret        - apiSecret
     * @param symbol           - symbol to subscribe
     * @param tradeBinListSize - size of the list to store tradeBin data from websocket, -1 to use default values
     */
    public WsImp(RestImp rest, boolean testnet, String apiKey, String apiSecret, String symbol, int tradeBinListSize) throws InterruptedException {
        this.container = ContainerProvider.getWebSocketContainer();
        this.g = new Gson();
        this.rest = rest;
        this.url = testnet ? WS_TESTNET : WS_MAINNET;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.userSession = null;
        this.heartbeatThread = null;
        this.data = new ConcurrentHashMap<>();
        this.orderQueue = new OrderAsyncThread(this);
        this.minReconnectTimeStamp = 0L;
        this.symbol = symbol;
        this.tradeBinListSize = tradeBinListSize > 0 ? tradeBinListSize : TRADE_BIN_MAX_LEN;
        // subscriptions to send to ws server
        this.subscriptions = "\"instrument:" + symbol + "\",\"order:" + symbol + "\",\"position:" + symbol + "\",\"execution:" + symbol + "\",\"tradeBin1m:" + symbol + "\",\"margin:*\"";

        // creates data structures to store data received by ws
        Order[] orderData = new Order[ORDER_MAX_LEN];
        Execution[] executionData = new Execution[EXEC_MAX_LEN];
        TradeBin[] tradeBinData = new TradeBin[tradeBinListSize];
        orderData[0] = new Order();
        executionData[0] = new Execution();
        tradeBinData[0] = new TradeBin();

        // initializes data in memory
        this.data.put(INSTRUMENT, new Instrument());
        this.data.put(ORDER, orderData);
        this.data.put(POSITION, new Position());
        this.data.put(EXECUTION, executionData);
        this.data.put(TRADE_BIN, tradeBinData);
        this.data.put(MARGIN, new UserMargin());

        this.connect();
        this.waitForData();
    }

    /**
     * Connects to BitMex web socket server
     */
    void connect() {
        try {
            this.container.connectToServer(this, URI.create(this.url));
        } catch (Exception e) {
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
        this.heartbeatThread = new HeartbeatThread(this);
        this.userSession = userSession;

        // gets orders for this symbol, trough http request
        Order[] restOrders = this.get_rest_orders();
        Collections.reverse(Arrays.asList(restOrders));
        this.data.put(ORDER, restOrders);

        // gets tradeBin data for this symbol, trough http request
        TradeBin[] tradeBinData = this.get_rest_last_1mCandles();
        Collections.reverse(Arrays.asList(tradeBinData));
        this.data.put(TRADE_BIN, tradeBinData);

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
        ThreadContext.put("ROUTINGKEY", symbol);
        if (!this.heartbeatThread.isInterrupted())
            this.heartbeatThread.interrupt();
        this.heartbeatThread = null;
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
    public void onError(Throwable throwable) throws InterruptedException {
        ThreadContext.put("ROUTINGKEY", symbol);
        logger.error(throwable.toString());
        Thread.sleep(3000);
        this.closeSession();
    }

    @Override
    public boolean closeSession() {
        if (isSessionOpen()) {
            try {
                this.userSession.close();
                return true;
            } catch (IOException e) {
                logger.error("Could not close user session.");
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean isSessionOpen() {
        return this.userSession != null;
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) throws InterruptedException {
        ThreadContext.put("ROUTINGKEY", symbol);
        if (!this.heartbeatThread.isInterrupted())
            this.heartbeatThread.interrupt();
        if (!isSessionOpen())
            return;
        this.heartbeatThread = new HeartbeatThread(this);

        //if it was an heartbeat message
        if (message.equalsIgnoreCase("pong"))
            return;

        JsonObject obj = g.fromJson(message, JsonObject.class);
        if (obj.has("subscribe")) {
            logger.debug("Subscribed successfully to " + obj.get("subscribe"));
        } else if (obj.has("status")) {
            logger.error(obj.get("error").getAsString());
            // Rate limited
            if (obj.get("status").getAsInt() == 429) {
                long waitTime = obj.get("meta").getAsJsonObject().get("retryAfter").getAsLong();
                logger.warn(String.format("Rate-limited, retrying on %d seconds.", waitTime));
                Thread.sleep(waitTime * 1000);
            }
        } else if (obj.has("table")) {
            String table = obj.get("table").getAsString();
            logger.debug(String.format("Received WS data: %s", message));
            switch (table) {
                case "instrument":
                    new Thread(() -> update_intrument(obj)).start();
                    break;
                case "orderBookL2":
                    new Thread(() -> update_orderBookL2(obj)).start();
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
        Instrument[] instrumentNewData = g.fromJson(obj.get("data"), Instrument[].class);

        if (action.equals("partial")) {
            this.data.put(INSTRUMENT, instrumentNewData[0]);
            synchronized (wsDataUpdate) {
                wsDataUpdate.notify();
            }
        } else if (action.equals("update")) {
            Instrument instrumentData = (Instrument) this.data.get(INSTRUMENT);
            String timestamp = instrumentNewData[0].getTimestamp();
            if (timestamp != null)
                check_latency(timestamp);
            instrumentData.update(instrumentNewData[0]);
            this.data.put(INSTRUMENT, instrumentData);
        }
    }

    /**
     * Checks latency on a websocket instrument update
     *
     * @param timestamp - timestamp of last update
     */
    private void check_latency(String timestamp) {
        long updateTime = TimeStamp.getTimestamp(timestamp);
        long latency = System.currentTimeMillis() - updateTime;
        synchronized (latencyLock) {
            if (latency > MAX_LATENCY && System.currentTimeMillis() > minReconnectTimeStamp) {
                minReconnectTimeStamp = System.currentTimeMillis() + FORCE_RECONNECT_INTERVAL;
                logger.warn(String.format("Reconnecting to websocket due to high latency of: %d Current timestamp: %d Next reconnect: %d", latency, System.currentTimeMillis(), minReconnectTimeStamp));
                this.closeSession();
            }
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'orderBookL2'
     *
     * @param obj - obj received from ws
     */
    private void update_orderBookL2(JsonObject obj) {
        String action = obj.get("action").getAsString();
        OrderBookL2[] data = g.fromJson(obj.get("data"), OrderBookL2[].class);
        if (action.equals("partial")) {
            this.data.put(ORDER_BOOK_L2, data);
        } else {
            OrderBookL2[] orderbookData = (OrderBookL2[]) this.data.get(ORDER_BOOK_L2);
            if (orderbookData == null)
                return;

            orderbookData = Arrays.copyOf(orderbookData, orderbookData.length);
            //checks every row on data array
            for (OrderBookL2 elem : data) {
                long id = elem.getId();
                long[] ids = new long[orderbookData.length];
                for (int i = 0; i < orderbookData.length; i++)
                    ids[i] = orderbookData[i].getId();

                int index = BinarySearch.binarySearchL(ids, 0, ids.length - 1, id);
                if (index == -1) {
                    orderbookData = (OrderBookL2[]) insert(BinarySearch.getIndexInSortedArray(ids, ids.length - 1, id), elem,
                            orderbookData);
                } else {
                    OrderBookL2 bookline = orderbookData[index];
                    if (action.equals("update"))
                        bookline.setSize(elem.getSize());
                    else if (action.equals("delete"))
                        bookline.setSize(0L);
                    bookline.setSide(elem.getSide());
                }
            }
            this.data.put(ORDER_BOOK_L2, orderbookData);
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'margin'
     *
     * @param obj - obj received from ws
     */
    private void update_margin(JsonObject obj) {
        String action = obj.get("action").getAsString();
        UserMargin[] dataRec = g.fromJson(obj.get("data"), UserMargin[].class);

        if (dataRec.length > 0 && (action.equals("update") || action.equals("partial"))) {
            UserMargin marginData = (UserMargin) this.data.get(MARGIN);
            marginData.update(dataRec[0]);
            this.data.put(MARGIN, marginData);
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'position'
     *
     * @param obj - obj received from ws
     */
    private void update_position(JsonObject obj) {
        String action = obj.get("action").getAsString();
        Position[] positionRec = g.fromJson(obj.get("data"), Position[].class);

        if (positionRec.length > 0 && (action.equals("update") || action.equals("partial"))) {
            Position positionData = (Position) this.data.get(POSITION);
            positionData.update(positionRec[0]);
            this.data.put(POSITION, positionData);
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'tradeBin1m'
     *
     * @param obj - obj received from ws
     */
    private void update_tradeBin1m(JsonObject obj) {
        String action = obj.get("action").getAsString();
        List<TradeBin> tradeBinRec = g.fromJson(obj.get("data"), new TypeToken<List<TradeBin>>() {
        }.getType());

        if (tradeBinRec.size() > 0 && action.equals("insert")) {
            List<TradeBin> tradeBinData = new ArrayList<>(Arrays.asList((TradeBin[]) this.data.get(TRADE_BIN)));
            for (TradeBin elem : tradeBinRec) {
                if (tradeBinData.size() == this.tradeBinListSize)
                    tradeBinData.remove(0);
                tradeBinData.add(elem);
            }
            this.data.put(TRADE_BIN, tradeBinData.toArray(new TradeBin[0]));
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'execution'
     *
     * @param obj - obj received from ws
     */
    private void update_execution(JsonObject obj) {
        String action = obj.get("action").getAsString();
        List<Execution> executionRec = g.fromJson(obj.get("data"), new TypeToken<List<Execution>>() {
        }.getType());

        if (executionRec.size() > 0 && action.equals("insert") || action.equals("partial")) {
            List<Execution> executionData = new ArrayList<>(Arrays.asList((Execution[]) this.data.get(EXECUTION)));
            for (Execution elem : executionRec) {
                if (executionData.size() == EXEC_MAX_LEN)
                    executionData.remove(0);
                executionData.add(elem);
            }
            this.data.put(EXECUTION, executionData.toArray(new Execution[0]));
        }
    }

    /**
     * Updates data in memory after receiving an ws message with table = 'order'
     *
     * @param obj - obj received from ws
     */
    protected void update_order(JsonObject obj) {
        String action = obj.get("action").getAsString();
        List<Order> orderData = new ArrayList<>(Arrays.asList((Order[]) this.data.get(ORDER)));
        List<Order> orderRec = g.fromJson(obj.get("data"), new TypeToken<List<Order>>() {
        }.getType());

        if (action.equals("insert")) {
            for (Order elem : orderRec) {
                if (orderData.size() == ORDER_MAX_LEN)
                    orderData.remove(0);
                orderData.add(elem);
            }
            this.data.put(ORDER, orderData.toArray(new Order[0]));
        } else if (action.equals("update")) {
            // iterates over data received
            for (Order elemRec : orderRec) {
                // iterates over orderData stored in memory/
                for (Order elemData : orderData) {
                    // if same order (same orderID)
                    if (elemData.equals(elemRec)) {
                        // updates data element
                        elemData.update(elemRec);
                        break;
                    }
                }
            }
            this.data.put(ORDER, orderData.toArray(new Order[0]));
        }
    }

    @Override
    public long getL2Size(float price) {
        OrderBookL2[] orderbookData = (OrderBookL2[]) this.data.get(ORDER_BOOK_L2);
        if (orderbookData == null)
            return -1L;
        float[] ids = new float[orderbookData.length];
        for (int i = 0; i < orderbookData.length; i++)
            ids[i] = orderbookData[i].getPrice();

        int index = BinarySearch.binarySearchF(ids, 0, ids.length - 1, price);
        return orderbookData[index].getSize();
    }

    @Override
    public Instrument get_instrument() {
        return (Instrument) this.data.get(INSTRUMENT);
    }

    @Override
    public TradeBin[] get_trabeBin1m() {
        return (TradeBin[]) this.data.get(TRADE_BIN);
    }

    @Override
    public OrderBookL2[] get_orderBookL2() {
        return (OrderBookL2[]) this.data.get(ORDER_BOOK_L2);
    }

    @Override
    public UserMargin get_margin() {
        return (UserMargin) this.data.get(MARGIN);
    }

    @Override
    public Execution[] get_execution() {
        return (Execution[]) this.data.get(EXECUTION);
    }

    @Override
    public Position get_position() {
        return (Position) this.data.get(POSITION);
    }

    @Override
    public Order[] get_openOrders(String orderIDPrefix) {
        List<Order> allOrders = new ArrayList<>(Arrays.asList((Order[]) this.data.get(ORDER)));
        return allOrders.stream()
                .filter(o -> o.getClOrdID() != null && o.getOrdStatus() != null && o.getClOrdID().startsWith(orderIDPrefix) && (o.getOrdStatus().equals("New") || o.getOrdStatus().equals("PartiallyFilled")))
                .toArray(Order[]::new);
    }

    @Override
    public Order[] get_filledOrders(String orderIDPrefix) {
        List<Order> allOrders = new ArrayList<>(Arrays.asList((Order[]) this.data.get(ORDER)));
        return allOrders.stream()
                .filter(o -> o.getClOrdID() != null && o.getOrdStatus() != null && o.getClOrdID().startsWith(orderIDPrefix) && o.getOrdStatus().equals("Filled"))
                .toArray(Order[]::new);
    }

    /**
     * Makes api rest call to get open orders
     *
     * @return Returns open orders for current symbol
     */
    private Order[] get_rest_orders() {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", this.symbol);
        params.addProperty("count", ORDER_MAX_LEN);
        params.addProperty("reverse", true);
        return this.rest.get_order(params);
    }

    /**
     * Makes api rest call to get last trade bucketed data
     *
     * @return Returns last candles data for current symbol
     */
    private TradeBin[] get_rest_last_1mCandles() {
        JsonObject params = new JsonObject();
        params.addProperty("binSize", "1m");
        params.addProperty("symbol", this.symbol);
        params.addProperty("count", this.tradeBinListSize);
        params.addProperty("reverse", true);
        return this.rest.get_trade_bucketed(params);
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
     * Adds an element to a array in a given index
     *
     * @param index - index on array to add element
     * @param val   - element to be added
     * @param arr   - array to be processed
     * @return newArray - new array
     */
    public static Object insert(int index, Object val, Object[] arr) {
        Object[] newArray = new Object[arr.length + 1];
        if (index >= 0) System.arraycopy(arr, 0, newArray, 0, index);
        newArray[index] = val;

        if (newArray.length - index + 1 >= 0)
            System.arraycopy(arr, index + 1 - 1, newArray, index + 1, newArray.length - index + 1);
        return newArray;
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
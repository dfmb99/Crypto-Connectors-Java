package binance.ws;

import binance.data.*;
import binance.rest.RestImp;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import utils.Tuple;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heartbeat thread that sends ping messages to the websocket server
 */
class ListenKeyExtenderThread extends Thread {
    private final UserStreamImp ws;
    private final String symbol;

    public ListenKeyExtenderThread(UserStreamImp ws, String symbol) {
        this.ws = ws;
        this.symbol = symbol;
        this.start();
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        while (!Thread.interrupted()) {
            if (System.currentTimeMillis() - startTime > 55 * 60000) {
                ThreadContext.put("ROUTINGKEY", symbol);
                ws.get_new_listen_key();
            }
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                this.interrupt();
            }
        }
    }
}


@ClientEndpoint
public class UserStreamImp implements UserStream{
    private static final Logger logger = LogManager.getLogger(UserStream.class.getName());
    private final WebSocketContainer container;
    private final Gson g;
    private final RestImp rest;
    private Session userSession;
    private final String url;
    private String listenKey;
    private final String symbol;
    private ListenKeyExtenderThread listenKeyExtender;
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
        this.symbol = symbol;
        get_new_listen_key();

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
        if(listenKeyExtender != null && !listenKeyExtender.isInterrupted())
            listenKeyExtender.interrupt();
        listenKeyExtender = null;
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
        logger.debug(message);

        WsUserData dataRec = g.fromJson(message, WsUserData.class);
        switch (dataRec.getEventType()) {
            case LISTEN_KEY_EXPIRED:
                this.listenKey = rest.start_user_stream().getListenKey();
                this.closeSession();
                break;
            case ACCOUNT_UPDATE:
                new Thread(() -> update_account(message) ).start();
                break;
            case ORDER_TRADE_UPDATE:
                new Thread(() -> update_order(message) ).start();
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void update_account(String data) {
        WsBalancePosition newData = g.fromJson(data, WsBalancePosition.class);
        Tuple<WsBalanceData[]> oldBalanceData = (Tuple<WsBalanceData[]>) wsData.get(BALANCES);
        Tuple<WsPositionData[]> oldPositionData = (Tuple<WsPositionData[]>) wsData.get(POSITIONS);

        if(oldBalanceData == null || newData.getEventTime() > oldBalanceData.timestamp) {
            Tuple<WsBalanceData[]> newBalanceData = new Tuple<>(newData.getEventTime(), newData.getBalancePositionData().getBalances());
            wsData.put(BALANCES, newBalanceData);
        }

        if(oldPositionData == null || newData.getEventTime() > oldPositionData.timestamp) {
            Tuple<WsPositionData[]> newPositionData = new Tuple<>(newData.getEventTime(), newData.getBalancePositionData().getPositions());
            wsData.put(POSITIONS, newPositionData);
        }
    }

    @SuppressWarnings("unchecked")
    private void update_order(String data) {
        WsOrder newData = g.fromJson(data, WsOrder.class);
        WsOrderData newOrdData = newData.getOrder();

        if(!newOrdData.getSymbol().equalsIgnoreCase(this.symbol))
            return;

        wsData.computeIfAbsent(ORDERS, k -> new ArrayList<Tuple<WsOrderData>>());
        List<Tuple<WsOrderData>> oldOrdData = (List<Tuple<WsOrderData>>) wsData.get(ORDERS);
        Tuple<WsOrderData> newOrdTuple = new Tuple<>(newData.getEventTime(), newOrdData);

        boolean found = false;
        for(Tuple<WsOrderData> singleOrdData: oldOrdData) {
            // only replaces order in memory if same clientOrderID and timestamp of update received is more recent than timestamp of update in memory
            if ( singleOrdData.y.getClientOrderID().equalsIgnoreCase(newOrdData.getClientOrderID()) && newOrdTuple.timestamp > singleOrdData.timestamp ) {
                oldOrdData.remove(singleOrdData);
                oldOrdData.add(newOrdTuple);
                found = true;
                break;
            }
        }

        if( !found ) {
            if(oldOrdData.size() == MAX_LEN_ORDER)
                oldOrdData.remove(0);
            oldOrdData.add(newOrdTuple);
        }
        Collections.sort(oldOrdData);
        wsData.put(ORDERS, oldOrdData);
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
     * Gets new listen key given
     */
    protected void get_new_listen_key() {
        this.listenKey = rest.start_user_stream().getListenKey();
        if(listenKeyExtender != null && !listenKeyExtender.isInterrupted())
            listenKeyExtender.interrupt();
        listenKeyExtender = new ListenKeyExtenderThread(this, symbol);
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

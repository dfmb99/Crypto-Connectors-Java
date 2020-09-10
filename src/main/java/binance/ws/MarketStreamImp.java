package binance.ws;

import binance.data.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ClientEndpoint
public class MarketStreamImp implements MarketStream {
    private static final Logger logger = LogManager.getLogger(MarketStreamImp.class.getName());
    private final WebSocketContainer container;
    private final Gson g;
    private Session userSession;
    private final String url;
    private final String subscriptions;
    private final String symbol;
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
    public MarketStreamImp(String url, String symbol) throws InterruptedException {
        this.container = ContainerProvider.getWebSocketContainer();
        this.g = new Gson();
        this.url = url;
        this.subscriptions = String.format("/stream?streams=%s@aggTrade/%s@markPrice/%s@kline_1m/%s@miniTicker/%s@forceOrder", symbol, symbol, symbol, symbol, symbol);
        this.userSession = null;
        this.wsData = new ConcurrentHashMap<>();
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
        logger.debug(message);

        WsData dataRec = g.fromJson(message, WsData.class);
        switch (dataRec.getStream().split("@")[1]) {
            case MINI_TICKER:
                new Thread(() -> update_miniTicker(dataRec.getData()) ).start();
                break;
            case KLINE_1M:
                new Thread(() -> update_kline(dataRec.getData()) ).start();
                break;
            case AGG_TRADE:
                new Thread(() -> update_aggTrade(dataRec.getData()) ).start();
                break;
            case MARK_PRICE:
                new Thread(() -> update_markPrice(dataRec.getData()) ).start();
                break;
            case LIQUIDATION:
                new Thread(() -> update_liquidation(dataRec.getData()) ).start();
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void update_liquidation(JsonObject data) {
        WsLiquidationRec newLiquidation = g.fromJson(data.toString(), WsLiquidationRec.class);
        List<WsLiquidationData> oldLiquidation = (List<WsLiquidationData>) wsData.get(LIQUIDATION);
        if(oldLiquidation != null) {
            int size = oldLiquidation.size();
            if(size == MAX_LEN_LIQUIDATION)
                oldLiquidation.remove(0);
            oldLiquidation.add(newLiquidation.getLiquidationData());
            wsData.put(LIQUIDATION, oldLiquidation);
        } else {
            List<WsLiquidationData> newLiquidationList = new ArrayList<>(MAX_LEN_LIQUIDATION);
            newLiquidationList.add(newLiquidation.getLiquidationData());
            wsData.put(LIQUIDATION, newLiquidationList);
        }
    }

    private void update_markPrice(JsonObject data) {
        WsMarkPrice oldMarkPrice = (WsMarkPrice) wsData.get(MARK_PRICE);
        WsMarkPrice newMarkPrice = g.fromJson(data.toString(), WsMarkPrice.class);
        long newTimestamp = newMarkPrice.getEventTime();
        check_latency(newTimestamp);
        if(oldMarkPrice != null) {
            long oldTimestamp = oldMarkPrice.getEventTime();
            if(newTimestamp > oldTimestamp)
                wsData.put(MARK_PRICE, newMarkPrice);
        } else {
            wsData.put(MARK_PRICE, newMarkPrice);
            synchronized (wsDataUpdate) {
                wsDataUpdate.notifyAll();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void update_aggTrade(JsonObject data) {
        List<WsAggTrade> oldAggTrade = (List<WsAggTrade>) wsData.get(AGG_TRADE);
        WsAggTrade newAggTrade = g.fromJson(data.toString(), WsAggTrade.class);
        if(oldAggTrade != null) {
            int size = oldAggTrade.size();
            if(size == MAX_LEN_AGG_TRADE)
                oldAggTrade.remove(0);
            oldAggTrade.add(newAggTrade);
            wsData.put(AGG_TRADE, oldAggTrade);
        } else {
            List<WsAggTrade> newAggTradeList = new ArrayList<>(MAX_LEN_AGG_TRADE);
            newAggTradeList.add(newAggTrade);
            wsData.put(AGG_TRADE, newAggTradeList);
        }
    }

    @SuppressWarnings("unchecked")
    private void update_kline(JsonObject data) {
        WsKlineRec newKline = g.fromJson(data.toString(), WsKlineRec.class);
        List<WsKlineData> oldKline = (List<WsKlineData>) wsData.get(KLINE_1M);
        if(oldKline != null) {
            int size = oldKline.size();
            if(size == MAX_LEN_KLINE)
                oldKline.remove(0);
            oldKline.add(newKline.getKline());
            wsData.put(KLINE_1M, oldKline);
        } else {
            List<WsKlineData> newKlineList = new ArrayList<>(MAX_LEN_KLINE);
            newKlineList.add(newKline.getKline());
            wsData.put(KLINE_1M, newKlineList);
        }
    }

    private void update_miniTicker(JsonObject data) {
        WsMiniTicker newMiniTicker = g.fromJson(data.toString(), WsMiniTicker.class);
        WsMiniTicker oldMiniTicker = (WsMiniTicker) wsData.get(MINI_TICKER);
        long newTimestamp = newMiniTicker.getEventTime();
        if(oldMiniTicker != null) {
            long oldTimestamp = oldMiniTicker.getEventTime();
            if(newTimestamp > oldTimestamp)
                wsData.put(MINI_TICKER, newMiniTicker);
        } else
            wsData.put(MINI_TICKER, newMiniTicker);
    }

    /**
     * Checks latency on a websocket instrument update
     *
     * @param timestamp - timestamp of last update
     */
    private void check_latency(Long timestamp) {
        long latency = System.currentTimeMillis() - timestamp;
        synchronized (latencyLock) {
            if (latency > MAX_LATENCY && System.currentTimeMillis() > minReconnectTimeStamp) {
                minReconnectTimeStamp = System.currentTimeMillis() + FORCE_RECONNECT_INTERVAL;
                ThreadContext.put("ROUTINGKEY", symbol);
                logger.warn(String.format("Reconnecting to websocket due to high latency of: %d Current timestamp: %d Next reconnect: %d", latency, System.currentTimeMillis(), minReconnectTimeStamp));
                this.closeSession();
            }
        }
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

    @Override
    public float get_mark_price() {
        WsMarkPrice data = (WsMarkPrice) wsData.get(MARK_PRICE);
        return data.getMarkPrice();
    }

    @Override
    public float get_funding_rate() {
        WsMarkPrice data = (WsMarkPrice) wsData.get(MARK_PRICE);
        return data.getFundingRate();
    }

    @Override
    public long get_next_funding_period() {
        WsMarkPrice data = (WsMarkPrice) wsData.get(MARK_PRICE);
        return data.getNextFundingTime();
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

package market_maker;

import bitmex.rest.RestImp;
import bitmex.ws.WsImp;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import utils.MathCustom;
import utils.SpotPricesTracker;
import utils.TimeStamp;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

class ExchangeInterface {
    private final static Logger LOGGER = Logger.getLogger(ExchangeInterface.class.getName());
    // Funding interval of perpetual swaps on miliseconds
    private final static long FUNDING_INTERVAL = 28800000;
    // days per anum
    private final static int DAYS_ANNUM = 365;
    // 1 day converted into milliseconds
    private final static long DAY_MS = 86400000L;

    private RestImp mexRest;
    private WsImp mexWs;
    private String orderIDPrefix;
    private String symbol;
    // class that deals with ticker data on other exchanges
    protected SpotPricesTracker spotPrices;
    // array that stores exchanges weights that are used as index in our symbol
    protected List<Float> weights;
    // Underlying symbol of contract
    private String underlyingSymbol;
    // if perpetual contract null, otherwise date of expiration
    private String expiry;
    // Thread that checks updates from other exchanges too see if data is valid and updates weights accordingly
    private IndexCheckThread indexThread;
    // timestamp on where indexes weights are updated
    protected long nextIndexUpdate;
    // tickSize of contract
    private float tickSize;

    public ExchangeInterface(String symbol) {
        this.symbol = symbol;
        this.orderIDPrefix = "mmbitmex";
        this.mexRest = new RestImp(Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, orderIDPrefix);
        this.mexWs = new WsImp(mexRest, Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, this.symbol);
        //this.spotPrices = new SpotPricesTracker(symbol);
        this.indexThread = null;

        // Initial data
        JsonObject instrument = mexWs.get_instrument().get(0).getAsJsonObject();
        this.expiry = instrument.get("expiry").isJsonNull() ? null : instrument.get("expiry").getAsString();
        this.underlyingSymbol = instrument.get("underlyingSymbol").getAsString();
        this.tickSize = instrument.get("tickSize").getAsFloat();
        // underlying symbol ( eg. 'XBT=' ) need to convert to ( '.BXBT')
        this.underlyingSymbol = String.format(".B%s", underlyingSymbol.split("=")[0]);
        //this.get_instrument_composite_index();

    }

    protected void get_instrument_composite_index() {
        // Interrupts index thread if thread exists
        if (this.indexThread != null && !this.indexThread.isInterrupted())
            this.indexThread.interrupt();

        this.nextIndexUpdate = TimeStamp.getTimestamp(this.mexRest.get_instrument("XBT:quarterly").get(0).getAsJsonObject().get("expiry").getAsString());
        // request to know composition of index on the symbol we are quoting
        JsonArray compIndRes = mexRest.get_instrument_compositeIndex(underlyingSymbol);
        List<String> exchangeRefs = new ArrayList<>();
        this.weights = new CopyOnWriteArrayList<>();
        for (JsonElement elem : compIndRes) {
            JsonObject obj = elem.getAsJsonObject();
            String reference = obj.get("reference").getAsString();
            if (reference.equalsIgnoreCase("BMI")) break;
            float weight = obj.get("weight").getAsFloat();
            exchangeRefs.add(reference);
            this.weights.add(weight);
        }

        spotPrices.addExchanges(exchangeRefs.toArray(new String[exchangeRefs.size()]));
        // creates new indexThread
        this.indexThread = new IndexCheckThread(this);
    }

    /**
     * Returns last price
     *
     * @return last price
     */
    public float get_last_price() {
        return this.mexWs.get_instrument().get(0).getAsJsonObject().get("lastPrice").getAsFloat();
    }

    /**
     * Returns bid price
     *
     * @return bid price
     */
    public float get_bid_price() {
        return this.mexWs.get_instrument().get(0).getAsJsonObject().get("bidPrice").getAsFloat();
    }

    /**
     * Returns ask price
     *
     * @return ask price
     */
    public float get_ask_price() {
        return this.mexWs.get_instrument().get(0).getAsJsonObject().get("askPrice").getAsFloat();
    }

    /**
     * Returns mid price
     *
     * @return mid price
     */
    public float get_mid_price() {
        return (get_ask_price() + get_bid_price()) / 2;
    }

    public float get_mark_price() {
        /**(For perpetual swaps)
         * Funding Basis = Funding Rate * (Time Until Funding / Funding Interval)
         * Fair Price    = Index Price * (1 + Funding Basis)
         *
         * (For futures contracts)
         * % Fair Basis = (Impact Mid Price / Index Price - 1) / (Time To Expiry / 365)
         * Fair Value   = Index Price * % Fair Basis * (Time to Expiry / 365)
         * Fair Price   = Index Price + Fair Value
         */

        float indexPrice = 0f;
        float[] lastPrices = this.spotPrices.get_last_price();
        for (int i = 0; i < lastPrices.length; i++) {
            indexPrice += lastPrices[i] * (float) this.weights.get(i);
        }

        JsonObject instrument = this.mexWs.get_instrument().get(0).getAsJsonObject();
        if (this.expiry == null) {
            float fundingRate = instrument.get("fundingRate").getAsFloat();
            long fundingTimestamp = TimeStamp.getTimestamp(instrument.get("fundingTimestamp").getAsString());
            float fundingBasis = fundingRate * (((float) fundingTimestamp - (float) System.currentTimeMillis()) / (float) FUNDING_INTERVAL);
            return indexPrice * (1.0f + fundingBasis);
        } else {
            float fairBasis = instrument.get("fairBasisRate").getAsFloat();
            long expiryTimestamp = TimeStamp.getTimestamp(this.expiry);
            float fairValue = indexPrice * fairBasis * (((float) expiryTimestamp - (float) System.currentTimeMillis()) / (float) DAY_MS / (float) DAYS_ANNUM);
            return indexPrice + fairValue;
        }
    }

    /**
     * Returns position size
     *
     * @return position size
     */
    public long get_position() {
        return this.mexWs.get_position().get(0).getAsJsonObject().get("currentQty").getAsLong();
    }

    /**
     * Get open buy orders
     *
     * @return open buy orders
     */
    public JsonArray get_open_buy_orders() {
        JsonArray ret = new JsonArray();
        JsonArray openOrders = this.mexWs.get_openOrders(this.orderIDPrefix);
        for (JsonElement elem : openOrders) {
            if (elem.getAsJsonObject().get("side").getAsString().equals("Buy"))
                ret.add(elem);
        }
        return ret;
    }

    /**
     * Get open sell orders
     *
     * @return open sell orders
     */
    public JsonArray get_open_sell_orders() {
        JsonArray ret = new JsonArray();
        JsonArray openOrders = this.mexWs.get_openOrders(this.orderIDPrefix);
        for (JsonElement elem : openOrders) {
            if (elem.getAsJsonObject().get("side").getAsString().equals("Sell"))
                ret.add(elem);
        }
        return ret;
    }

    /**
     * Get margin used (ratio between available margin and margin balance)
     *
     * @return margin used
     */
    public float get_margin_used() {
        JsonObject margin = this.mexWs.get_margin().get(0).getAsJsonObject();
        return (float) margin.get("availableMargin").getAsLong() / margin.get("marginBalance").getAsLong();
    }


    /**
     * Returns price of an order
     *
     * @param order - JsonObject
     * @return price
     */
    public float get_order_price(JsonObject order) {
        return order.get("price").getAsFloat();
    }

    /**
     * Returns lowest open buy order orderID
     *
     * @param order - JsonObject
     * @return orderID
     */
    public String get_orderID(JsonObject order) {
        return order.get("orderID").getAsString();
    }

    /**
     * Returns highest open buy order
     *
     * @return order
     */
    public JsonObject get_highest_buy() {
        JsonObject highestBuy = new JsonObject();
        JsonArray orders = this.get_open_buy_orders();
        for (JsonElement currOrd : orders) {
            if (highestBuy.keySet().size() < 1 || currOrd.getAsJsonObject().get("price").getAsFloat() > highestBuy.get("price").getAsFloat())
                highestBuy = currOrd.getAsJsonObject();
        }
        return highestBuy;
    }

    /**
     * Returns lowest open sell order
     *
     * @return order
     */
    public JsonObject get_lowest_sell() {
        JsonObject lowestSell = new JsonObject();
        JsonArray orders = this.get_open_sell_orders();
        for (JsonElement currOrd : orders) {
            if (lowestSell.keySet().size() < 1 || currOrd.getAsJsonObject().get("price").getAsFloat() > lowestSell.get("price").getAsFloat())
                lowestSell = currOrd.getAsJsonObject();
        }
        return lowestSell;
    }

    /**
     * Places an order
     *
     * @param order - order to be placed
     * @return JsonObject - response of request
     */
    public JsonObject place_order(JsonObject order) {
        return this.mexRest.post_order(order);
    }

    /**
     * Places multiple orders as bulk
     *
     * @param orders - orders to be placed
     * @return JsonObject - response of request
     */
    public JsonArray place_order_bulk(JsonArray orders) {
        JsonObject params = new JsonObject();
        params.addProperty("orders", String.valueOf(orders));
        return this.mexRest.post_order_bulk(params);
    }

    /**
     * Amends an order
     *
     * @param order - order to be amended
     * @return JsonObject - response of request
     */
    public JsonObject amend_order(JsonObject order) {
        return this.mexRest.put_order(order);
    }

    /**
     * Amends multiple orders as bulk
     *
     * @param orders - orders to be amended
     * @return JsonObject - response of request
     */
    public JsonArray amend_order_bulk(JsonArray orders) {
        JsonObject params = new JsonObject();
        params.addProperty("orders", String.valueOf(orders));
        return this.mexRest.put_order_bulk(params);
    }

    /**
     * Returns trade bucketed data 1minute
     *
     * @return JSONArray
     */
    public JsonArray get_tradeBin1m() {
        return this.mexWs.get_trabeBin1m();
    }
}

class MarketMakerManager {
    private final static Logger LOGGER = Logger.getLogger(MarketMakerManager.class.getName());
    private ExchangeInterface e;
    private String symbol;

    public MarketMakerManager(String symbol) {
        e = new ExchangeInterface(symbol);
        this.symbol = symbol;
    }

    /**
     * Prepares a limit order
     *
     * @param orderQty - orderQty, if negative sell order
     * @param price    - price to place the order
     * @return
     */
    public JsonObject prepare_limit_order(long orderQty, float price) {
        JsonObject params = new JsonObject();
        String execInst = Settings.POST_ONLY ? "ParticipateDoNotInitiate" : "";

        params.addProperty("symbol", this.symbol);
        params.addProperty("orderQty", orderQty);
        params.addProperty("price", price);
        params.addProperty("ordType", "Limit");
        params.addProperty("execInst", execInst);

        return params;
    }

    /**
     * Prepares a market order
     *
     * @param orderQty - orderQty, if negative sell order
     * @return order - order built
     */
    public JsonObject prepare_market_order(long orderQty) {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", this.symbol);
        params.addProperty("orderQty", orderQty);
        params.addProperty("ordType", "Market");

        return params;
    }

    /**
     * Returns volume index of current contract
     *
     * @return volIndex as float
     */
    private float get_volume_index() {
        JsonArray arr = e.get_tradeBin1m();
        float[] closeArr = new float[arr.size() - 1];
        for (int i = 1; i < arr.size(); i++) {
            closeArr[i - 1] = (float) Math.log(arr.get(i).getAsJsonObject().get("close").getAsFloat() / arr.get(i - 1).getAsJsonObject().get("close").getAsFloat());
        }
        return (float) (MathCustom.calculateSD(closeArr) * Math.sqrt(closeArr.length));
    }

    /**
     * Calculates skew depending on current position size
     *
     * @return skew
     */
    private float get_position_skew() {
        float skew = 0;
        long currPos = e.get_position();

        if (currPos > 0)
            skew = (float) ((-1 + Math.pow(2.4, Math.abs(currPos) / Settings.ORDER_SIZE / 4)) * get_volume_index() * 0.8 * -1);
        else if (currPos < 0)
            skew = (float) ((-1 + Math.pow(2.4, Math.abs(currPos) / Settings.ORDER_SIZE / 4)) * get_volume_index() * 0.8);

        return skew;
    }

    private float get_new_bid_price() {
        float quoteMidPrice = e.get_mark_price() * (1 + get_position_skew());
        return quoteMidPrice * (1 - get_volume_index());
    }

    private float get_new_ask_price() {
        float quoteMidPrice = e.get_mark_price() * (1 + get_position_skew());
        return quoteMidPrice * (1 + get_volume_index());
    }

    private void check_current_spread() {
        float fairPrice = e.get_mark_price();
        JsonObject buy = e.get_highest_buy();
        JsonObject sell = e.get_lowest_sell();

        // big volatility changes can cause we quoting a spread too wide, so we amend orders to tight the spread
        if ((buy.keySet().size() > 0 && sell.keySet().size() > 0) && (get_spread(get_new_bid_price(), fairPrice) > get_spread(e.get_order_price(buy), fairPrice) * Settings.SPREAD_MAINTAIN_RATIO) &&
                (get_spread(get_new_ask_price(), fairPrice) > get_spread(e.get_order_price(sell), fairPrice) * Settings.SPREAD_MAINTAIN_RATIO)) {
            LOGGER.info(String.format("Spread too wide, amending orders. Current volume index: %d", get_volume_index()));
            amend_orders();
        }
    }

    /**
     * Amends order pair that is closer to midPrice with current position skew, if orders exist, otherwise does nothing
     */
    private void amend_orders() {
        JsonArray orders = new JsonArray();
        JsonObject buy = e.get_highest_buy();
        JsonObject sell = e.get_lowest_sell();

        if (buy.keySet().size() > 0) {
            JsonObject newBuy = new JsonObject();
            newBuy.addProperty("orderID", buy.get("orderID").getAsString());
            newBuy.addProperty("price", get_new_bid_price());
            orders.add(newBuy);
            LOGGER.info(String.format("Amending %s order from %d to %d", buy.get("side").getAsString(), buy.get("price").getAsFloat(), newBuy.get("price").getAsFloat()));
        }
        if (sell.keySet().size() > 0) {
            JsonObject newSell = new JsonObject();
            newSell.addProperty("orderID", sell.get("orderID").getAsString());
            newSell.addProperty("price", get_new_ask_price());
            orders.add(newSell);
            LOGGER.info(String.format("Amending %s order from %d to %d", sell.get("side").getAsString(), sell.get("price").getAsFloat(), newSell.get("price").getAsFloat()));
        }

        if (!Settings.DRY_RUN && orders.size() > 0)
            e.amend_order_bulk(orders);
    }

    /**
     * Returns the spread between two prices
     *
     * @param p1 - price 1
     * @param p2 - price 2
     * @return spread
     */
    private float get_spread(float p1, float p2) {
        return Math.abs(p1 / p2) / p2;
    }

    /**
     * Checks current open orders, and replaces/places new orders if any are missing
     */
    private void converge_orders() {
        JsonArray orders = new JsonArray();
        JsonArray buys = e.get_open_buy_orders();
        JsonArray sells = e.get_open_sell_orders();

        if (buys.size() < 1) {
            JsonObject newBuy = prepare_limit_order(Settings.ORDER_SIZE, get_new_bid_price());
            orders.add(newBuy);
            LOGGER.info(String.format("Creating buy order of %d contracts at %d", newBuy.get("orderQty").getAsLong(), newBuy.get("price").getAsFloat()));
        }

        if (sells.size() < 1) {
            JsonObject newSell = prepare_limit_order(-Settings.ORDER_SIZE, get_new_ask_price());
            orders.add(newSell);
            LOGGER.info(String.format("Creating sell order of %d contracts at %d", newSell.get("orderQty").getAsLong(), newSell.get("price").getAsFloat()));
        }

        if (!Settings.DRY_RUN && orders.size() > 0)
            e.place_order_bulk(orders);

    }

    private void sanity_check() {

    }

    private void run_loop() {
        while (true) {
            converge_orders();
            check_current_spread();
        }
    }
}

/**
 * Heartbeat thread that checks price feeds from other exchanges
 */
class IndexCheckThread extends Thread {
    private final static Logger LOGGER = Logger.getLogger(IndexCheckThread.class.getName());

    private ExchangeInterface e;
    // updated prices
    private float[] currPrices;
    // original exchange weights
    private float[] origWeights;
    // removed exchange weights
    private float[] remWeights;
    // updated timestamps
    private long[] timeStamps;
    // number of active indexes
    private int activeIndexes;

    public IndexCheckThread(ExchangeInterface e) {
        this.e = e;
        currPrices = e.spotPrices.get_last_price();
        origWeights = new float[e.weights.size()];
        remWeights = new float[e.weights.size()];
        timeStamps = new long[e.weights.size()];
        long now = System.currentTimeMillis();
        for (int i = 0; i < origWeights.length; i++) {
            origWeights[i] = e.weights.get(i);
            timeStamps[i] = now;
            remWeights[i] = 0.0f;
        }
        activeIndexes = origWeights.length;
        this.start();
    }

    @Override
    public void run() {
        float[] newData;
        float remW;
        float addedWeights;
        int i;
        long now;
        while (!Thread.interrupted()) {
            remW = 0.0f;
            now = System.currentTimeMillis();
            newData = e.spotPrices.get_last_price();

            // Indexes weights get updated after quarterly futures expiry + 5 seconds
            if (System.currentTimeMillis() > e.nextIndexUpdate + 5000) {
                LOGGER.info("Updating index weights.");
                this.interrupt();
                e.get_instrument_composite_index();
            }

            for (i = 0; i < currPrices.length; i++) {
                // if data received is different than data we have, update timestamp array
                if (newData[i] != currPrices[i]) {
                    timeStamps[i] = now;
                    if (e.weights.get(i) == 0.0f) {
                        float v = origWeights[i] / (float) activeIndexes;
                        LOGGER.warning(String.format("Invalid exchange updated price, removing from other valid exchanges: %d", v));
                        for (int j = 0; j < currPrices.length; j++) {
                            float k = e.weights.get(j);
                            if (k > 0.0f)
                                e.weights.set(j, k - v);
                        }
                        e.weights.set(i, origWeights[i]);
                    }
                }
                //check if index needs to be removed
                if (System.currentTimeMillis() - timeStamps[i] > 900000) {
                    LOGGER.warning(String.format("Removing exchange at index %d due to not receiving price updates for 15 minutes.", i));
                    activeIndexes--;
                    //removed weight needs to be distributed by the rest
                    remW += e.weights.get(i);
                    // set this weight to 0
                    e.weights.set(i, 0.0f);
                }
            }
            if (remW > 0.0f) {
                addedWeights = remW / (float) activeIndexes;
                LOGGER.warning(String.format("Adding to every other valid exchange: %d", addedWeights));
                for (i = 0; i < currPrices.length; i++) {
                    float v = e.weights.get(i);
                    if (v > 0.0f)
                        e.weights.set(i, v + addedWeights);
                }
            }
            currPrices = newData.clone();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                //Do nothing
            }
        }
    }
}

public class MarketMaker {

    private final static Logger LOGGER = Logger.getLogger(MarketMaker.class.getName());

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %2$s %4$s: %5$s%6$s%n");
        MarketMakerManager m = new MarketMakerManager(Settings.SYMBOL);

    }

    private void fileWatcher() throws IOException, InterruptedException {
        WatchService watchService
                = FileSystems.getDefault().newWatchService();

        Path path = Paths.get(System.getProperty("user.dir") + "\\src\\main\\java\\");

        path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        WatchKey key;
        while ((key = watchService.take()) != null) {
            // prevents multiple same events
            Thread.sleep(3000);
            for (WatchEvent<?> event : key.pollEvents()) {
                String fileChanged = event.context().toString();
                if (!fileChanged.equals("logs"))
                    LOGGER.warning("Event kind:" + event.kind() + ". File affected: " + fileChanged);
            }
            key.reset();
        }
    }
}

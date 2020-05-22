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
    // Funding interval of perpetual swaps on miliseconds
    private final static long FUNDING_INTERVAL = 28800000;
    // days per anum
    private final static int DAYS_ANNUM = 365;
    // 1 day converted into milliseconds
    private final static long DAY_MS = 86400000L;

    private final RestImp mexRest;
    private final WsImp mexWs;
    private final String orderIDPrefix;
    // class that deals with ticker data on other exchanges
    protected SpotPricesTracker spotPrices;
    // array that stores exchanges weights that are used as index in our symbol
    protected List<Float> weights;
    // Underlying symbol of contract
    private String underlyingSymbol;
    // if perpetual contract null, otherwise date of expiration
    private final String expiry;
    // Thread that checks updates from other exchanges too see if data is valid and updates weights accordingly
    private IndexCheckThread indexThread;
    // timestamp on where indexes weights are updated
    protected long nextIndexUpdate;
    // tickSize of contract
    private final float tickSize;

    public ExchangeInterface() {
        this.orderIDPrefix = Settings.ORDER_ID_PREFIX;
        this.mexRest = new RestImp(Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, this.orderIDPrefix);
        this.mexWs = new WsImp(mexRest, Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, Settings.SYMBOL);
        this.spotPrices = new SpotPricesTracker(Settings.SYMBOL);
        this.indexThread = null;

        // Initial data
        JsonObject instrument = mexWs.get_instrument().get(0).getAsJsonObject();
        this.expiry = instrument.get("expiry").isJsonNull() ? null : instrument.get("expiry").getAsString();
        this.underlyingSymbol = instrument.get("underlyingSymbol").getAsString();
        this.tickSize = instrument.get("tickSize").getAsFloat();
        // underlying symbol ( eg. 'XBT=' ) need to convert to ( '.BXBT')
        this.underlyingSymbol = String.format(".B%s", underlyingSymbol.split("=")[0]);
        this.get_instrument_composite_index();

    }

    /**
     * Checks if websocket connection is open
     *
     * @return true if open, false otherwise
     */
    protected boolean isWebsocketOpen() {
        return this.mexWs.isSessionOpen();
    }

    /**
     * Returns tick size of contract
     *
     * @return tickSize as float
     */
    protected float get_tickSize() {
        return this.tickSize;
    }

    /**
     * Get current instrument indixes composition and weights
     */
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

        spotPrices.addExchanges(exchangeRefs.toArray(new String[0]));
        // creates new indexThread
        this.indexThread = new IndexCheckThread(this);
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
        /*
         * (For perpetual swaps)
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
            indexPrice += lastPrices[i] * this.weights.get(i);
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
     * Http request to get open orders
     *
     * @return JsonArray of response
     */
    public JsonArray rest_get_open_orders() {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", Settings.SYMBOL);
        params.addProperty("filter", "{\"ordStatus.isTerminated\": false}");
        params.addProperty("count", 100);
        params.addProperty("reverse", true);
        return this.mexRest.get_order(params);
    }

    /**
     * Gets open orders as a array of JsonArrays
     *
     * @return JsonArray[0] -> open buy orders / JsonArray[1] -> open sell orders
     */
    public JsonArray[] get_open_orders() {
        JsonArray[] arr = new JsonArray[2];
        arr[0] = new JsonArray();
        arr[1] = new JsonArray();

        JsonArray openOrders = this.mexWs.get_openOrders(this.orderIDPrefix);
        for (JsonElement elem : openOrders) {
            if (elem.getAsJsonObject().get("side").getAsString().equals("Buy"))
                arr[0].add(elem);
            else
                arr[1].add(elem);
        }
        return arr;
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
     * Returns JsonObject[] w/ top of the book orders (bid and ask order more more closer to midPrice)
     *
     * @return JsonObject[0] -> highest open buy order  / JsonObject[1] -> lowest open sell order
     */
    public JsonObject[] get_topBook_orders() {
        JsonArray[] orders = this.get_open_orders();
        JsonObject highestBuy = new JsonObject();
        JsonObject lowestSell = new JsonObject();

        for (JsonElement currOrd : orders[0]) {
            if (highestBuy.keySet().size() < 1 || currOrd.getAsJsonObject().get("price").getAsFloat() > highestBuy.get("price").getAsFloat())
                highestBuy = currOrd.getAsJsonObject();
        }
        for (JsonElement currOrd : orders[1]) {
            if (lowestSell.keySet().size() < 1 || currOrd.getAsJsonObject().get("price").getAsFloat() < lowestSell.get("price").getAsFloat())
                lowestSell = currOrd.getAsJsonObject();
        }

        return new JsonObject[]{highestBuy, lowestSell};
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
        params.add("orders", orders);
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
        params.add("orders", orders);
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
    private final ExchangeInterface e;

    public MarketMakerManager() {
        e = new ExchangeInterface();
        run_loop();
    }

    /**
     * Prepares a limit order
     *
     * @param orderQty - orderQty, if negative sell order
     * @param price    - price to place the order
     * @return order
     */
    public JsonObject prepare_limit_order(long orderQty, float price) {
        JsonObject params = new JsonObject();
        String execInst = Settings.POST_ONLY ? "ParticipateDoNotInitiate" : "";

        params.addProperty("symbol", Settings.SYMBOL);
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
        params.addProperty("symbol", Settings.SYMBOL);
        params.addProperty("orderQty", orderQty);
        params.addProperty("ordType", "Market");

        return params;
    }

    /**
     * Returns spread index of current contract, based on historical volatility
     *
     * @return volIndex as float
     */
    private float get_spread_index() {
        JsonArray arr = e.get_tradeBin1m();
        float[] closeArr = new float[arr.size() - 1];
        float midPrice = e.get_mid_price();

        for (int i = 1; i < arr.size(); i++) {
            closeArr[i - 1] = (float) Math.log(arr.get(i).getAsJsonObject().get("close").getAsFloat() / arr.get(i - 1).getAsJsonObject().get("close").getAsFloat());
        }
        float currVolIndex = (float) (MathCustom.calculateSD(closeArr) * Math.sqrt(closeArr.length));
        float minimumSpread = get_spread(midPrice + Settings.MIN_SPREAD_TICKS * e.get_tickSize(), midPrice);

        LOGGER.fine(String.format("Current volume index: %f", Math.max(currVolIndex, minimumSpread)));
        return Math.max(currVolIndex, minimumSpread);
    }

    /**
     * Calculates skew depending on current position size
     *
     * @return skew
     */
    private float get_position_skew() {
        long currPos = e.get_position();
        float skew = 0;

        float c = (-1f + (float) Math.pow(2.4, (float) Math.abs(currPos) / (float) Settings.ORDER_SIZE / 4f)) * get_spread_index() * 0.8f;
        if (currPos > 0)
            skew = c * -1f;
        else if (currPos < 0)
            skew = c;

        return skew;
    }

    /**
     * Returns new float array that contains new order prices based on current skew
     * @return [0] -> bid price / [1] -> ask price
     */
    private float[] get_new_order_prices() {
        float[] prices = new float[2];
        float quoteMidPrice = e.get_mark_price() * (1f + get_position_skew());
        prices[0] = (float) MathCustom.roundToFraction(quoteMidPrice * (1f - get_spread_index()), e.get_tickSize());
        prices[1] = (float) MathCustom.roundToFraction(quoteMidPrice * (1f + get_spread_index()), e.get_tickSize());
        return prices;
    }

    /**
     * Checks current order spreads to markPrice, and amends them if necessary
     */
    private void check_current_spread() {
        float fairPrice = e.get_mark_price();
        float[] newPrices = get_new_order_prices();
        JsonObject[] topBookOrds = e.get_topBook_orders();

        // big volatility changes can cause we quoting a spread too wide, so we amend orders to tight the spread
        if ((topBookOrds[0].keySet().size() > 0 && topBookOrds[1].keySet().size() > 0) && (get_spread(e.get_order_price(topBookOrds[0]), fairPrice) > get_spread(newPrices[0], fairPrice) * Settings.SPREAD_MAINTAIN_RATIO) &&
                (get_spread(e.get_order_price(topBookOrds[1]), fairPrice) > get_spread(newPrices[1], fairPrice) * Settings.SPREAD_MAINTAIN_RATIO)) {
            LOGGER.info(String.format("Spread too wide, amending orders. Current volume index: %f", get_spread_index()));
            amend_orders();
        }
    }

    /**
     * Amends order pair that is closer to midPrice with current position skew, if orders exist, otherwise does nothing
     */
    private void amend_orders() {
        JsonArray orders = new JsonArray();
        float[] newPrices = get_new_order_prices();
        JsonObject[] topBookOrds = e.get_topBook_orders();

        if (topBookOrds[0].keySet().size() > 0) {
            JsonObject newBuy = new JsonObject();
            newBuy.addProperty("orderID", topBookOrds[0].get("orderID").getAsString());
            newBuy.addProperty("price", newPrices[0]);
            orders.add(newBuy);
            LOGGER.info(String.format("Amending %s order from %f to %f", topBookOrds[0].get("side").getAsString(), topBookOrds[0].get("price").getAsFloat(), newBuy.get("price").getAsFloat()));
        }
        if (topBookOrds[1].keySet().size() > 0) {
            JsonObject newSell = new JsonObject();
            newSell.addProperty("orderID", topBookOrds[1].get("orderID").getAsString());
            newSell.addProperty("price", newPrices[1]);
            orders.add(newSell);
            LOGGER.info(String.format("Amending %s order from %f to %f", topBookOrds[1].get("side").getAsString(), topBookOrds[1].get("price").getAsFloat(), newSell.get("price").getAsFloat()));
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
        return Math.abs(p1 - p2) / p2;
    }

    /**
     * Checks current open orders, and replaces/places new orders if any are missing
     */
    private void converge_orders() {
        JsonArray orders = new JsonArray();
        float[] newPrices = get_new_order_prices();
        JsonObject[] topBookOrds = e.get_topBook_orders();

        // place new buy order, if no buy order is opened
        if (topBookOrds[0].keySet().size() < 1) {
            JsonObject newBuy = prepare_limit_order(Settings.ORDER_SIZE, newPrices[0]);
            orders.add(newBuy);
            LOGGER.info(String.format("Creating buy order of %d contracts at %f", newBuy.get("orderQty").getAsLong(), newBuy.get("price").getAsFloat()));

            // amends current sell order if there is a sell order opened
            if (topBookOrds[1].keySet().size() > 0) {
                JsonObject newSell = new JsonObject();
                newSell.addProperty("orderID", topBookOrds[1].get("orderID").getAsString());
                newSell.addProperty("price", newPrices[1]);
                LOGGER.info(String.format("Amending %s order from %f to %f", topBookOrds[1].get("side").getAsString(), topBookOrds[1].get("price").getAsFloat(), newSell.get("price").getAsFloat()));
                if (!Settings.DRY_RUN)
                    e.amend_order(newSell);
            }
        }

        // place new sell order, if no sell order is opened
        if (topBookOrds[1].keySet().size() < 1) {
            JsonObject newSell = prepare_limit_order(-Settings.ORDER_SIZE, newPrices[1]);
            orders.add(newSell);
            LOGGER.info(String.format("Creating sell order of %d contracts at %f", newSell.get("orderQty").getAsLong(), newSell.get("price").getAsFloat()));

            // amends current buy order if there is a buy order opened
            if (topBookOrds[0].keySet().size() > 0) {
                JsonObject newBuy = new JsonObject();
                newBuy.addProperty("orderID", topBookOrds[0].get("orderID").getAsString());
                newBuy.addProperty("price", newPrices[0]);
                LOGGER.info(String.format("Amending %s order from %f to %f", topBookOrds[0].get("side").getAsString(), topBookOrds[0].get("price").getAsFloat(), newBuy.get("price").getAsFloat()));
                if (!Settings.DRY_RUN)
                    e.amend_order(newBuy);
            }
        }

        if (!Settings.DRY_RUN) {
            if (orders.size() > 0)
                e.place_order_bulk(orders);
            else
                check_current_spread();
        }
    }

    private void sanity_check() {

    }

    private void run_loop() {
        while (true) {
            try {
                if (e.isWebsocketOpen()) {
                    converge_orders();
                    Thread.sleep(Settings.LOOP_INTERVAL);
                }
            } catch (InterruptedException interruptedException) {
                // Do nothing
            }
        }
    }
}

/**
 * Heartbeat thread that checks price feeds from other exchanges
 */
class IndexCheckThread extends Thread {
    private final static Logger LOGGER = Logger.getLogger(IndexCheckThread.class.getName());

    private final ExchangeInterface e;
    // updated prices
    private float[] currPrices;
    // original exchange weights
    private final float[] origWeights;
    // updated timestamps
    private final long[] timeStamps;
    // number of active indexes
    private int activeIndexes;

    public IndexCheckThread(ExchangeInterface e) {
        this.e = e;
        currPrices = e.spotPrices.get_last_price();
        origWeights = new float[e.weights.size()];
        timeStamps = new long[e.weights.size()];
        long now = System.currentTimeMillis();
        for (int i = 0; i < origWeights.length; i++) {
            origWeights[i] = e.weights.get(i);
            timeStamps[i] = now;
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
                        LOGGER.warning(String.format("Invalid exchange updated price, removing from other valid exchanges: %f", v));
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
                LOGGER.warning(String.format("Adding to every other valid exchange: %f", addedWeights));
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

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %2$s %4$s: %5$s%6$s%n");
        MarketMakerManager m = new MarketMakerManager();

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

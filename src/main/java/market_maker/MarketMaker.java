package market_maker;

import bitmex.rest.RestImp;
import bitmex.ws.WsImp;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.MathCustom;
import utils.SpotPricesTracker;
import utils.TimeStamp;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

class ExchangeInterface {
    // Funding interval of perpetual swaps on miliseconds
    private final static long FUNDING_INTERVAL = 28800000;
    // days per anum
    private final static int DAYS_ANNUM = 365;
    // 1 day converted into milliseconds
    private final static long DAY_MS = 86400000L;

    private final RestImp mexRest;
    private final WsImp mexWs;
    private final String symbol;
    private final String orderIDPrefix;
    // class that deals with ticker data on other exchanges
    protected SpotPricesTracker spotPrices;
    // array that stores exchanges weights that are used as index in our symbol
    private List<Float> weights;
    // Underlying symbol of contract
    private String underlyingSymbol;
    // if perpetual contract null, otherwise date of expiration
    private final String expiry;
    // timestamp on where indexes weights are updated
    protected long nextIndexUpdate;
    // tickSize of contract
    private final float tickSize;

    public ExchangeInterface(String symbol) {
        this.symbol = symbol;
        this.orderIDPrefix = Settings.ORDER_ID_PREFIX;
        this.mexRest = new RestImp(Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, this.orderIDPrefix);
        this.mexWs = new WsImp(mexRest, Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, symbol);
        this.spotPrices = new SpotPricesTracker(symbol);

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
     * Gets instrument state
     */
    protected String get_instrument_state() {
        return this.mexRest.get_instrument(this.symbol).get(0).getAsJsonObject().get("state").getAsString();
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
        this.nextIndexUpdate = TimeStamp.getTimestamp(this.mexRest.get_instrument("XBT:quarterly").get(0).getAsJsonObject().get("expiry").getAsString());
        // request to know composition of index on the symbol we are quoting
        JsonArray compIndRes = mexRest.get_instrument_compositeIndex(underlyingSymbol);
        List<String> exchangeRefs = new ArrayList<>();
        this.weights = new ArrayList<>();
        for (JsonElement elem : compIndRes) {
            JsonObject obj = elem.getAsJsonObject();
            String reference = obj.get("reference").getAsString();
            if (reference.equalsIgnoreCase("BMI")) break;
            float weight = obj.get("weight").getAsFloat();
            exchangeRefs.add(reference);
            this.weights.add(weight);
        }

        spotPrices.addExchanges(exchangeRefs.toArray(new String[0]));
    }

    /**
     * Returns bid price
     *
     * @return bid price
     */
    protected float get_bid_price() {
        return this.mexWs.get_instrument().get(0).getAsJsonObject().get("bidPrice").getAsFloat();
    }

    /**
     * Returns ask price
     *
     * @return ask price
     */
    protected float get_ask_price() {
        return this.mexWs.get_instrument().get(0).getAsJsonObject().get("askPrice").getAsFloat();
    }

    /**
     * Returns mid price
     *
     * @return mid price
     */
    protected float get_mid_price() {
        return (get_ask_price() + get_bid_price()) / 2;
    }

    /**
     * Gets mark price from BitMex websocket
     */
    protected float get_ws_mark_price() {
        return this.mexWs.get_instrument().get(0).getAsJsonObject().get("markPrice").getAsFloat();
    }

    /**
     * Calculates and returns mark price from spot exchanges data
     *
     * @return mark price
     */
    protected float get_mark_price() {
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
    protected long get_position() {
        JsonElement currQty = this.mexWs.get_position().get(0).getAsJsonObject().get("currentQty");
        if (currQty != null)
            return currQty.getAsLong();
        else
            return 0L;
    }

    /**
     * Http request to get open sell orders
     *
     * @return JsonArray of response
     */
    protected JsonArray rest_get_open_sell_orders() {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", this.symbol);
        params.addProperty("filter", JsonParser.parseString("{\"ordStatus.isTerminated\": false, \"side\": \"Sell\"}").toString());
        params.addProperty("count", 25);
        params.addProperty("reverse", true);
        return this.mexRest.get_order(params);
    }

    /**
     * Http request to get open buy orders
     *
     * @return JsonArray of response
     */
    protected JsonArray rest_get_open_buy_orders() {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", this.symbol);
        params.addProperty("filter", JsonParser.parseString("{\"ordStatus.isTerminated\": false, \"side\": \"Buy\"}").toString());
        params.addProperty("count", 25);
        params.addProperty("reverse", true);
        return this.mexRest.get_order(params);
    }

    /**
     * Gets open orders as a array of JsonArrays
     *
     * @return JsonArray[0] -> open buy orders / JsonArray[1] -> open sell orders
     */
    protected JsonArray[] get_open_orders() {
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
     * Gets filled orders as a array of JsonArrays
     *
     * @return JsonArray[0] -> filled buy orders / JsonArray[1] -> filled sell orders
     */
    protected JsonArray[] get_filled_orders() {
        JsonArray[] arr = new JsonArray[2];
        arr[0] = new JsonArray();
        arr[1] = new JsonArray();

        JsonArray filledOrders = this.mexWs.get_filledOrders(this.orderIDPrefix);
        for (JsonElement elem : filledOrders) {
            if (elem.getAsJsonObject().get("side").getAsString().equals("Buy"))
                arr[0].add(elem);
            else
                arr[1].add(elem);
        }
        return arr;
    }

    /**
     * Returns true if buy order w/ orderID is filled
     *
     * @param orderID - orderID of buy order
     * @return true if buy order w/ orderID is filled, false otherwise
     */
    protected boolean is_buy_order_filled(String orderID) {
        JsonArray[] filledOrders = get_filled_orders();
        for (JsonElement elem : filledOrders[0]) {
            if (elem.getAsJsonObject().get("orderID").getAsString().equals(orderID))
                return true;
        }
        return false;
    }

    /**
     * Returns true if sell order w/ orderID is filled
     *
     * @param orderID - orderID of sell order
     * @return true if sell order w/ orderID is filled, false otherwise
     */
    protected boolean is_sell_order_filled(String orderID) {
        JsonArray[] filledOrders = get_filled_orders();
        for (JsonElement elem : filledOrders[1]) {
            if (elem.getAsJsonObject().get("orderID").getAsString().equals(orderID))
                return true;
        }
        return false;
    }

    /**
     * Cancels all open orders on this contract
     */
    protected void cancel_all_orders() {
        JsonObject params = new JsonObject();
        JsonArray openOrders = this.mexWs.get_openOrders(this.orderIDPrefix);
        String[] toCancel = new String[openOrders.size()];

        for (int i = 0; i < openOrders.size(); i++)
            toCancel[i] = openOrders.get(i).getAsJsonObject().get("orderID").toString();

        params.addProperty("orderID", Arrays.toString(toCancel));
        if (toCancel.length > 0)
            this.mexRest.del_order(params);
    }

    /**
     * Cancel orders passed on params
     *
     * @param params - orders IDs to be canceled
     */
    protected void cancel_orders(JsonObject params) {
        this.mexRest.del_order(params);
    }

    /**
     * Get margin used (ratio between available margin and margin balance)
     *
     * @return margin used
     */
    protected float get_margin_used() {
        JsonObject margin = this.mexWs.get_margin().get(0).getAsJsonObject();
        return 1f - (margin.get("availableMargin").getAsFloat() / margin.get("marginBalance").getAsFloat());
    }

    /**
     * Get margin balance on account
     *
     * @return margin balance
     */
    protected float get_margin_balance() {
        JsonObject margin = this.mexWs.get_margin().get(0).getAsJsonObject();
        return margin.get("marginBalance").getAsFloat() * (float) Math.pow(10, -8);
    }

    /**
     * Returns price of an order
     *
     * @param order - JsonObject
     * @return price
     */
    protected float get_order_price(JsonObject order) {
        return order.get("price").getAsFloat();
    }

    /**
     * Returns lowest open buy order orderID
     *
     * @param order - JsonObject
     * @return orderID
     */
    protected String get_orderID(JsonObject order) {
        return order.get("orderID").getAsString();
    }

    /**
     * Returns JsonObject[] w/ top of the book orders (bid and ask order more more closer to midPrice)
     *
     * @return JsonObject[0] -> highest open buy order  / JsonObject[1] -> lowest open sell order
     */
    protected JsonObject[] get_topBook_orders() {
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
    protected JsonObject place_order(JsonObject order) {
        return this.mexRest.post_order(order);
    }

    /**
     * Places multiple orders as bulk
     *
     * @param orders - orders to be placed
     * @return JsonObject - response of request
     */
    protected JsonArray place_order_bulk(JsonArray orders) {
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
    protected JsonObject amend_order(JsonObject order) {
        return this.mexRest.put_order(order);
    }

    /**
     * Amends multiple orders as bulk
     *
     * @param orders - orders to be amended
     * @return JsonObject - response of request
     */
    protected JsonArray amend_order_bulk(JsonArray orders) {
        JsonObject params = new JsonObject();
        params.add("orders", orders);
        return this.mexRest.put_order_bulk(params);
    }

    /**
     * Returns trade bucketed data 1minute
     *
     * @return JSONArray
     */
    protected JsonArray get_tradeBin1m() {
        return this.mexWs.get_trabeBin1m();
    }
}

class MarketMakerManager {
    private final static Logger LOGGER = Logger.getLogger(MarketMakerManager.class.getName());
    private final ExchangeInterface e;
    private final String symbol;
    // sanity checks are made when this timestamp is hit
    private long sanityCheckStamp;
    // mark price warning logs are made when this timestamp is hit
    private long markPriceLogStamp;
    // list w/ orderIDs of open buy orders made by the algorithm
    private List<String> openBuyOrds;
    // list w/ orderIDs of open sell orders made by the algorithm
    private List<String> openSellOrds;

    public MarketMakerManager(String symbol) {
        e = new ExchangeInterface(symbol);
        this.symbol = symbol;
        this.sanityCheckStamp = System.currentTimeMillis() + Settings.SANITY_CHECK_INTERVAL;
        this.markPriceLogStamp = 0L;
        this.openBuyOrds = new ArrayList<>();
        this.openSellOrds = new ArrayList<>();
        run_loop();
    }

    /**
     * Prepares a limit order
     *
     * @param orderQty - orderQty, if negative sell order
     * @param price    - price to place the order
     * @return order
     */
    private JsonObject prepare_limit_order(long orderQty, float price) {
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
    private JsonObject prepare_market_order(long orderQty) {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", this.symbol);
        params.addProperty("orderQty", orderQty);
        params.addProperty("ordType", "Market");

        return params;
    }

    /**
     * Checks if short position limit is exceeded
     *
     * @return true if is exceeded, false otherwise
     */
    private boolean short_position_limit_exceeded() {
        if (!Settings.CHECK_POSITION_LIMITS)
            return false;
        return e.get_position() <= Settings.MIN_POSITION;
    }

    /**
     * Checks if long position limit is exceeded
     *
     * @return true if is exceeded, false otherwise
     */
    private boolean long_position_limit_exceeded() {
        if (!Settings.CHECK_POSITION_LIMITS)
            return false;
        return e.get_position() >= Settings.MAX_POSITION;
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
        float currVolIndex = (MathCustom.calculateSD(closeArr) * (float) Math.sqrt(closeArr.length));
        currVolIndex = currVolIndex * (float) Math.sqrt(1f / ((float) closeArr.length / Settings.SPREAD_INDEX));
        float minimumSpread = get_spread_abs(midPrice + (float) Settings.MIN_SPREAD_TICKS * e.get_tickSize(), midPrice);

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
     *
     * @return [0] -> bid price / [1] -> ask price
     */
    private float[] get_new_order_prices() {
        float[] prices = new float[2];

        float quoteMidPrice = get_mark_price() * (1f + get_position_skew());
        prices[0] = MathCustom.roundToFraction(quoteMidPrice * (1f - get_spread_index()), e.get_tickSize());
        prices[1] = MathCustom.roundToFraction(quoteMidPrice * (1f + get_spread_index()), e.get_tickSize());
        return prices;
    }

    /**
     * Gets mark price to be used in algorithm calculations
     *
     * @return mark price
     */
    private float get_mark_price() {
        // mark price calculated by algorithm
        float calculatedMarkPrice = e.get_mark_price();
        // mark price received in bitmex websocket
        float wsMarkPrice = e.get_ws_mark_price();
        // spread between calculated mark price and mark price received by websocket
        float spread = get_spread_abs(calculatedMarkPrice, wsMarkPrice);

        if (spread > 0.005f) {
            long now = System.currentTimeMillis();
            // to prevent spam of logging messages (only prints every x ms depending on user settings)
            if (now > markPriceLogStamp) {
                markPriceLogStamp = now + Settings.MARK_PRICE_LOG_INTERVAL;
                LOGGER.info(String.format("Using mark price from BitMex websocket: (websocket) %f, (calculated) %f, (spread) %f", wsMarkPrice, calculatedMarkPrice, spread));
            }
            return wsMarkPrice;
        }

        return calculatedMarkPrice;
    }

    /**
     * Checks current order spreads to markPrice, and amends them if necessary
     */
    private void check_current_spread() throws InterruptedException {
        float fairPrice = get_mark_price();
        float[] newPrices = get_new_order_prices();
        JsonObject[] topBookOrds = e.get_topBook_orders();

        // big volatility changes can cause we quoting a spread too wide, so we amend orders to tight the spread
        if ((topBookOrds[0].keySet().size() > 0 && topBookOrds[1].keySet().size() > 0) && (get_spread_abs(e.get_order_price(topBookOrds[0]), fairPrice) > get_spread_abs(newPrices[0], fairPrice) * Settings.SPREAD_MAINTAIN_RATIO) &&
                (get_spread_abs(e.get_order_price(topBookOrds[1]), fairPrice) > get_spread_abs(newPrices[1], fairPrice) * Settings.SPREAD_MAINTAIN_RATIO)) {
            LOGGER.info(String.format("Spread too wide, amending orders. Current volume index: %f", get_spread_index()));
            amend_orders();
        }
    }

    /**
     * Amends order pair that is closer to midPrice with current position skew, if orders exist, otherwise does nothing
     */
    private void amend_orders() throws InterruptedException {
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

        if (!Settings.DRY_RUN && orders.size() > 0) {
            e.amend_order_bulk(orders);
            Thread.sleep(Settings.REST_INTERVAL);
            print_status();
        }
    }

    /**
     * Returns the absolute spread between two prices
     *
     * @param p1 - price 1
     * @param p2 - price 2
     * @return spread
     */
    public static float get_spread_abs(float p1, float p2) {
        return Math.abs(p1 - p2) / p2;
    }

    /**
     * Returns the spread between two prices
     *
     * @param p1 - price 1
     * @param p2 - price 2
     * @return spread
     */
    public static float get_spread(float p1, float p2) {
        return (p1 - p2) / p2;
    }

    /**
     * Checks current open orders, and replaces/places new orders if any are missing
     */
    private void converge_orders() throws InterruptedException {
        JsonArray orders = new JsonArray();
        float[] newPrices = get_new_order_prices();
        JsonObject[] topBookOrds = e.get_topBook_orders();

        this.openBuyOrds.removeIf(e::is_buy_order_filled);
        this.openSellOrds.removeIf(e::is_sell_order_filled);

        // place new buy order, if no buy order is opened
        if (this.openBuyOrds.size() < 1 && topBookOrds[0].keySet().size() < 1 && !long_position_limit_exceeded()) {
            JsonObject newBuy = prepare_limit_order(Settings.ORDER_SIZE, newPrices[0]);
            orders.add(newBuy);
            LOGGER.info(String.format("Creating buy order of %d contracts at %f (%f)", newBuy.get("orderQty").getAsLong(), newPrices[0], get_spread(newPrices[0], get_mark_price())));

            // amends current sell order if there is a sell order opened
            if (this.openSellOrds.size() > 0 && topBookOrds[1].keySet().size() > 0) {
                JsonObject newSell = new JsonObject();
                newSell.addProperty("orderID", topBookOrds[1].get("orderID").getAsString());
                newSell.addProperty("price", newPrices[1]);
                LOGGER.info(String.format("Amending %s order from %f to %f (%f)", topBookOrds[1].get("side").getAsString(), topBookOrds[1].get("price").getAsFloat(), newPrices[1], get_spread(newPrices[1], get_mark_price())));
                if (!Settings.DRY_RUN)
                    e.amend_order(newSell);
            }
        }

        // place new sell order, if no sell order is opened
        if (this.openSellOrds.size() < 1 && topBookOrds[1].keySet().size() < 1 && !short_position_limit_exceeded()) {
            JsonObject newSell = prepare_limit_order(-Settings.ORDER_SIZE, newPrices[1]);
            orders.add(newSell);
            LOGGER.info(String.format("Creating sell order of %d contracts at %f (%f)", newSell.get("orderQty").getAsLong(), newPrices[1], get_spread(newPrices[1], get_mark_price())));

            // amends current buy order if there is a buy order opened
            if (this.openBuyOrds.size() > 0 && topBookOrds[0].keySet().size() > 0) {
                JsonObject newBuy = new JsonObject();
                newBuy.addProperty("orderID", topBookOrds[0].get("orderID").getAsString());
                newBuy.addProperty("price", newPrices[0]);
                LOGGER.info(String.format("Amending %s order from %f to %f (%f)", topBookOrds[0].get("side").getAsString(), topBookOrds[0].get("price").getAsFloat(), newPrices[0], get_spread_abs(newPrices[0], get_mark_price())));
                if (!Settings.DRY_RUN)
                    e.amend_order(newBuy);
            }
        }

        if (!Settings.DRY_RUN) {
            orders = delete_orders_high_margin(orders);
            if (orders.size() > 0) {
                // makes http request to BitMex servers to place orders
                JsonArray ordResp = e.place_order_bulk(orders);
                // iterates over JsonArray response
                for (JsonElement elem : ordResp) {
                    JsonObject obj = elem.getAsJsonObject();
                    // if order placed with success and still open add it to open orders in local memory
                    if (obj.has("ordStatus") && (obj.get("ordStatus").getAsString().equals("New") || obj.get("ordStatus").getAsString().equals("PartiallyFilled"))) {
                        String orderID = obj.get("orderID").getAsString();
                        if (obj.get("side").getAsString().equals("Buy"))
                            this.openBuyOrds.add(orderID);
                        else if (obj.get("side").getAsString().equals("Sell"))
                            this.openSellOrds.add(orderID);
                    }
                }
                Thread.sleep(Settings.REST_INTERVAL);
                print_status();
            } else
                check_current_spread();
        }
    }

    /**
     * Receives orders to be placed and if too much margin is being used deletes the orders that would increase the position preventing liquidations
     *
     * @param orders - orders to be checked, deletes orders that would increase position size if too high margin being used
     */
    private JsonArray delete_orders_high_margin(JsonArray orders) {
        long position = e.get_position();
        float marginUsed = e.get_margin_used();
        JsonElement[] toRmv = new JsonElement[orders.size()];
        int counter = 0;

        if (marginUsed > Settings.MAX_MARGIN_USED && position > 0L) { // only places sell orders
            for (JsonElement elem : orders) {
                if (elem.getAsJsonObject().get("orderQty").getAsLong() > 0L)
                    toRmv[counter++] = elem;
            }
        } else if (marginUsed > Settings.MAX_MARGIN_USED && position < 0L) { // only places buy orders
            for (JsonElement elem : orders) {
                if (elem.getAsJsonObject().get("orderQty").getAsLong() < 0L)
                    toRmv[counter++] = elem;
            }
        } else if (marginUsed > Settings.MAX_MARGIN_USED && position == 0L) { // we do not place orders
            for(JsonElement elem : orders)
                toRmv[counter++] = elem;
        }

        for (int i = 0; i < counter; i++)
            orders.remove(toRmv[i]);

        if(counter > 0)
            LOGGER.warning(String.format("Margin used is above %f. Current: %f", Settings.MAX_MARGIN_USED, marginUsed));

        return orders;
    }

    protected void cancel_all_orders() {
        LOGGER.info("Canceling all open orders.");
        e.cancel_all_orders();
    }

    private void sanity_check() {
        JsonArray[] openOrders = e.get_open_orders();
        List<String> toCancel = new ArrayList<>();

        if (!e.get_instrument_state().equals("Open")) {
            LOGGER.warning(String.format("Instrument %s is not open.", this.symbol));
            System.exit(1);
        } else if (short_position_limit_exceeded()) {
            LOGGER.warning("Short delta limit exceeded.");
            LOGGER.warning(String.format("Current position: %d Minimum position: %d", e.get_position(), Settings.MIN_POSITION));
            e.cancel_all_orders();
            //System.exit(1);
        } else if (long_position_limit_exceeded()) {
            LOGGER.warning("Long delta limit exceeded.");
            LOGGER.warning(String.format("Current position: %d Maximum position: %d", e.get_position(), Settings.MAX_POSITION));
            e.cancel_all_orders();
            //System.exit(1);
        } else if (openOrders[0].size() > 1) { // checks how many bids on the orderbook
            LOGGER.warning(String.format("%d buy orders will be canceled.", openOrders[0].size() - 1));
            // highest buy orderID
            String highestBuyID = e.get_topBook_orders()[0].get("orderID").toString();
            for (JsonElement elem : openOrders[0]) {
                String orderID = elem.getAsJsonObject().get("orderID").toString();
                if (!highestBuyID.equals(orderID))
                    toCancel.add(orderID);
            }
        } else if (openOrders[1].size() > 1) { // checks how many asks on the orderbook
            LOGGER.warning(String.format("%d ask orders will be canceled.", openOrders[1].size() - 1));

            // lowest sell orderID
            String lowestSellID = e.get_topBook_orders()[1].get("orderID").toString();
            for (JsonElement elem : openOrders[1]) {
                String orderID = elem.getAsJsonObject().get("orderID").toString();
                if (!lowestSellID.equals(orderID))
                    toCancel.add(orderID);
            }
        } else if(e.rest_get_open_buy_orders().size() < 1 && this.openBuyOrds.size() > 0) {
            LOGGER.warning(String.format("No buy order opened, probably canceled. It was expected to be open."));
            this.openBuyOrds.remove(0);
        } else if (e.rest_get_open_sell_orders().size() < 1 && this.openSellOrds.size() > 0) {
            LOGGER.warning(String.format("No sell order opened, probably canceled. It was expected to be open."));
            this.openSellOrds.remove(0);
        }

        if (toCancel.size() > 0) {
            JsonObject obj = new JsonObject();
            String[] params = new String[toCancel.size()];
            toCancel.toArray(params);
            obj.addProperty("orderID", Arrays.toString(params));
            e.cancel_orders(obj);
        }

    }

    private void print_status() {
        JsonArray[] openOrders = e.get_open_orders();

        LOGGER.info(String.format("Position: %d", e.get_position()));
        LOGGER.info(String.format("Margin balance: %f", e.get_margin_balance()));
        LOGGER.info(String.format("Margin used: %f%%", e.get_margin_used() * 100f));
        LOGGER.info(String.format("Fair price: %f", get_mark_price()));
        LOGGER.info(String.format("Spread index: %f", get_spread_index()));
        LOGGER.info(String.format("Skew: %f", get_position_skew()));
        LOGGER.info(String.format("Open orders: bids %d, asks %d", openOrders[0].size(), openOrders[1].size()));
    }

    private void run_loop() {
        while (true) {
            try {
                //sanity check
                long now = System.currentTimeMillis();
                if (now > sanityCheckStamp) {
                    sanityCheckStamp = now + Settings.SANITY_CHECK_INTERVAL;
                    sanity_check();
                }
                // Indexes weights get updated after quarterly futures expiry + 5 seconds
                if (System.currentTimeMillis() > e.nextIndexUpdate + 5000) {
                    LOGGER.info("New quarterly expiry, updating index weights.");
                    e.get_instrument_composite_index();
                }
                // if websocket connection open update orders
                if (e.isWebsocketOpen()) {
                    converge_orders();
                    Thread.sleep(Settings.LOOP_INTERVAL);
                }
            } catch (InterruptedException interruptedException) {
                LOGGER.warning("Algorithm execution interrupted.");
                e.cancel_all_orders();
            }
        }
    }
}

public class MarketMaker {

    private final static Logger LOGGER = Logger.getLogger(MarketMaker.class.getName());

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %2$s %4$s: %5$s%6$s%n");
        loggingConfig();
        LOGGER.info(String.format("Starting execution in %s with PID: %d", Settings.SYMBOL, ProcessHandle.current().pid()));
        new MarketMakerManager(Settings.SYMBOL);
    }

    private static void loggingConfig() {
        Logger log = Logger.getLogger("");
        Handler fileHandler = null;
        try {
            fileHandler = new FileHandler(String.format("./logs/%s.log", Settings.SYMBOL));
            SimpleFormatter simple = new SimpleFormatter();
            fileHandler.setFormatter(simple);

            log.addHandler(fileHandler);//adding Handler for file
        } catch (IOException e) {
            // Do nothing
        }
    }

    private static void fileWatcher() throws IOException, InterruptedException {
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
                LOGGER.info("Event kind:" + event.kind() + ". File affected: " + fileChanged);
            }
            key.reset();
        }
    }
}

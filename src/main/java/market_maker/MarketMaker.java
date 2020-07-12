package market_maker;

import Exceptions.NotImplementedException;
import bitmex.data.Instrument;
import bitmex.data.Order;
import bitmex.data.TradeBin;
import bitmex.data.UserMargin;
import bitmex.rest.RestImp;
import bitmex.ws.WsImp;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import utils.MathCustom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

class ExchangeInterface {

    private final int i;
    private final RestImp mexRest;
    private final WsImp mexWs;
    private final String orderIDPrefix;
    private final float tickSize;

    public ExchangeInterface(int settingsIndex) {
        this.i = settingsIndex;
        this.orderIDPrefix = Settings.ORDER_ID_PREFIX;
        this.mexRest = new RestImp(Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, this.orderIDPrefix);
        this.mexWs = new WsImp(mexRest, Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, Settings.SYMBOL[i], Settings.TRADE_BIN_SIZE[i]);

        // http request to get instrument data
        Instrument instrument = get_instrument_contract();
        this.tickSize = instrument.getTickSize();
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
     * Returns instrument data of contract
     *
     * @return Instrument object of this contract
     */
    protected Instrument get_instrument_contract() {
        return this.mexWs.get_instrument();
    }

    /**
     * Returns tick size of contract
     *
     * @return tickSize as float
     */
    protected float get_tickSize() {
        return this.tickSize;
    }

    /*
     * Calculates and returns mark price from spot exchanges data
     *
     * @return mark price
     *
    protected float get_mark_price() {
         *
         * (For perpetual swaps)
         * Funding Basis = Funding Rate * (Time Until Funding / Funding Interval)
         * Fair Price    = Index Price * (1 + Funding Basis)
         *
         * (For futures contracts)
         * % Fair Basis = (Impact Mid Price / Index Price - 1) / (Time To Expiry / 365)
         * Fair Value   = Index Price * % Fair Basis * (Time to Expiry / 365)
         * Fair Price   = Index Price + Fair Value
         *

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
    }*/

    /**
     * Returns bid price
     *
     * @return bid price
     */
    protected float get_bid_price() {
        return this.mexWs.get_instrument().getBidPrice();
    }

    /**
     * Returns ask price
     *
     * @return ask price
     */
    protected float get_ask_price() {
        return this.mexWs.get_instrument().getAskPrice();
    }

    /**
     * Returns mid price
     *
     * @return mid price
     */
    protected float get_mid_price() {
        return this.mexWs.get_instrument().getMidPrice();
    }

    /**
     * Gets mark price from BitMex websocket
     */
    protected float get_mark_price() {
        return this.mexWs.get_instrument().getMarkPrice();
    }

    /**
     * Returns position size
     *
     * @return position size
     */
    protected long get_position_size() {
        Long currQty = this.mexWs.get_position().getCurrentQty();
        return Objects.requireNonNullElse(currQty, 0L);
    }

    /**
     * Returns position entry price, -1 if no position
     *
     * @return position entry price, -1 if no position
     */
    protected float get_position_entry() {
        Float currQty = this.mexWs.get_position().getAvgEntryPrice();
        return Objects.requireNonNullElse(currQty, -1F);
    }

    /**
     * Http request to get open orders
     *
     * @return List<List < Order>>[0] -> open buy orders / List<List<Order>>[1] -> open sell orders
     */
    protected List<List<Order>> rest_get_open_orders() {
        List<Order> buyOrd = new ArrayList<>(100);
        List<Order> sellOrd = new ArrayList<>(100);

        JsonObject params = new JsonObject();
        params.addProperty("symbol", Settings.SYMBOL[i]);
        params.addProperty("filter", "{\"ordStatus.isTerminated\":false}");
        params.addProperty("count", 100);
        params.addProperty("reverse", true);

        Order[] openOrders = this.mexRest.get_order(params);
        for (Order e : openOrders) {
            if (e.getSide().equals("Buy"))
                buyOrd.add(e);
            else
                sellOrd.add(e);
        }

        List<List<Order>> toRet = new ArrayList<>(2);
        toRet.add(buyOrd);
        toRet.add(sellOrd);

        return toRet;
    }

    /**
     * Gets open orders from websocket
     *
     * @return List<List < Order>>[0] -> open buy orders / List<List<Order>>[1] -> open sell orders
     */
    protected List<List<Order>> get_open_orders() {
        return getOrderLists(this.mexWs.get_openOrders(this.orderIDPrefix));
    }

    /**
     * Gets filled orders from websocket
     *
     * @return List<List < Order>>[0] -> filled buy orders / List<List<Order>>[1] -> filled sell orders
     */
    protected List<List<Order>> get_filled_orders() {
        return getOrderLists(this.mexWs.get_filledOrders(this.orderIDPrefix));
    }

    /**
     * Gets orders from websocket, and splits them into two arrays based on their side of the order book
     *
     * @return List<List < Order>>[0] -> filled buy orders / List<List<Order>>[1] -> filled sell orders
     */
    private List<List<Order>> getOrderLists(Order[] orders) {
        List<Order> buyOrd = new ArrayList<>(orders.length);
        List<Order> sellOrd = new ArrayList<>(orders.length);
        for (Order e : orders) {
            if (e.getSide().equals("Buy"))
                buyOrd.add(e);
            else
                sellOrd.add(e);
        }

        List<List<Order>> toRet = new ArrayList<>(2);
        toRet.add(buyOrd);
        toRet.add(sellOrd);

        return toRet;
    }

    /**
     * Returns true if buy order w/ orderID is filled
     *
     * @param orderID - orderID of buy order
     * @return true if buy order w/ orderID is filled, false otherwise
     */
    protected boolean is_buy_order_filled(String orderID) {
        List<List<Order>> filledOrders = get_filled_orders();
        for (Order elem : filledOrders.get(0)) {
            if (elem.getOrderID().equals(orderID))
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
        List<List<Order>> filledOrders = get_filled_orders();
        for (Order elem : filledOrders.get(1)) {
            if (elem.getOrderID().equals(orderID))
                return true;
        }
        return false;
    }

    /**
     * Amends an order
     *
     * @param order - order to be amended
     */
    protected void amend_order(JsonObject order) {
        this.mexRest.put_order(order);
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
        UserMargin margin = this.mexWs.get_margin();
        return 1f - (margin.getAvailableMargin() / margin.getMarginBalance());
    }

    /**
     * Get margin balance in XBT
     *
     * @return margin balance in XBT
     */
    protected float get_margin_balance() {
        return this.mexWs.get_margin().getMarginBalance() * (float) Math.pow(10, -8);
    }

    /**
     * Get wallet balance
     *
     * @return wallet balance
     */
    protected float get_wallet_balance() {
        return this.mexWs.get_margin().getWalletBalance();
    }

    /**
     * Returns top of the book orders (highest bid and lowest ask)
     *
     * @return Order[0] -> highest open buy order  / Order[1] -> lowest open sell order
     */
    protected Order[] get_topBook_orders() {
        List<List<Order>> orders = this.get_open_orders();
        Order highestBuy = null, lowestSell = null;

        for (Order bid : orders.get(0)) {
            if (highestBuy == null || bid.getPrice() > highestBuy.getPrice())
                highestBuy = bid;
        }
        for (Order ask : orders.get(1)) {
            if (lowestSell == null || ask.getPrice() < lowestSell.getPrice())
                lowestSell = ask;
        }

        return new Order[]{highestBuy, lowestSell};
    }

    /**
     * Places multiple orders as bulk
     *
     * @param orders - orders to be placed
     * @return Order array - response of request
     */
    protected Order[] place_order_bulk(JsonArray orders) {
        JsonObject params = new JsonObject();
        params.add("orders", orders);
        return this.mexRest.post_order_bulk(params);
    }

    /**
     * Amends multiple orders as bulk
     *
     * @param orders - orders to be amended
     */
    protected void amend_order_bulk(JsonArray orders) {
        JsonObject params = new JsonObject();
        params.add("orders", orders);
        this.mexRest.put_order_bulk(params);
    }

    /**
     * Returns trade 1m bucketed data from websocket
     *
     * @return TradeBin array
     */
    protected TradeBin[] get_tradeBin1m() {
        return this.mexWs.get_trabeBin1m();
    }
}

class MarketMakerManager {
    private final static long DAY_TO_MILLISECONDS = 86400000L;
    private final static long MINUTE_TO_MILLISECONDS = 60000L;
    private final static long WEEK_TO_MILLISECONDS = MINUTE_TO_MILLISECONDS * 5;
    //private final static long WEEK_TO_MILLISECONDS = 604800000L;
    private final static int API_REST_INTERVAL = 500;

    private final static Logger LOGGER = Logger.getLogger(MarketMakerManager.class.getName());
    // Settings file index
    private final int i;
    private final ExchangeInterface e;
    private long fillsCounter;
    // sanity checks are made when this timestamp is hit
    private long sanityCheckStamp;
    // timestamp used to reset fills counter every 24 hours;
    private long fillsStamp;
    // timestamp used to calculate order size
    private long calcOrderSizeStamp;
    // list w/ orderIDs of open buy orders made by the algorithm
    private final List<String> openBuyOrds;
    // list w/ orderIDs of open sell orders made by the algorithm
    private final List<String> openSellOrds;
    private long orderSize;
    private long maxPosition;
    private long minPosition;

    public MarketMakerManager(int settingsIndex) throws InterruptedException, NotImplementedException {
        this.i = settingsIndex;
        this.e = new ExchangeInterface(settingsIndex);
        this.fillsCounter = 0L;
        this.fillsStamp = System.currentTimeMillis() + DAY_TO_MILLISECONDS;
        this.sanityCheckStamp = System.currentTimeMillis() + MINUTE_TO_MILLISECONDS;
        this.calcOrderSizeStamp = System.currentTimeMillis() + WEEK_TO_MILLISECONDS;
        this.openBuyOrds = new ArrayList<>(2);
        this.openSellOrds = new ArrayList<>(2);
        List<List<Order>> openOrders = e.rest_get_open_orders();
        for (Order elem : openOrders.get(0))
            this.openBuyOrds.add(elem.getOrderID());
        for (Order elem : openOrders.get(1))
            this.openSellOrds.add(elem.getOrderID());

        if(!Settings.FLEXIBLE_ORDER_SIZE[i]) {
            this.orderSize = Settings.ORDER_SIZE[i];
            this.maxPosition = Settings.MAX_POSITION[i];
            this.minPosition = Settings.MIN_POSITION[i];
        } else
            calc_pos_max_delta();

        run_loop();
    }

    private void calc_pos_max_delta() throws NotImplementedException, InterruptedException {
        // instrument of contract we are quoting
        Instrument instrument = e.get_instrument_contract();

        float deltaMaxPos = Settings.POS_MAX_MARGIN[i] * e.get_wallet_balance() / instrument.getInitMargin() / 100f;
        long orderSize;

        if(instrument.getQuanto()) {
            orderSize = (long) (deltaMaxPos / Settings.POSITION_FACTOR[i] / (instrument.getMultiplier() * instrument.getMarkPrice()) );
        }else if(instrument.getInverse()) {
            orderSize = (long) (deltaMaxPos / Settings.POSITION_FACTOR[i] * instrument.getMarkPrice() / instrument.getMultiplier());
        }else if(!instrument.getQuanto() && !instrument.getInverse()){
            orderSize = (long) (deltaMaxPos / Settings.POSITION_FACTOR[i] / instrument.getMultiplier());
        } else
            throw new NotImplementedException(String.format("%s Contract not implemented.", Settings.SYMBOL[i]));

        this.orderSize = Math.abs(orderSize); // inverse contracts have negative multipliers
        this.maxPosition = this.orderSize* (long) Settings.POSITION_FACTOR[i];
        this.minPosition = - this.maxPosition;
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
        String execInst = Settings.POST_ONLY[i] ? "ParticipateDoNotInitiate" : "";

        params.addProperty("symbol", Settings.SYMBOL[i]);
        params.addProperty("orderQty", orderQty);
        params.addProperty("price", price);
        params.addProperty("ordType", "Limit");
        params.addProperty("execInst", execInst);

        return params;
    }

    /*
     * Prepares a market order
     *
     * @param orderQty - orderQty, if negative sell order
     * @return order - order built
    private JsonObject prepare_market_order(long orderQty) {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", this.symbol);
        params.addProperty("orderQty", orderQty);
        params.addProperty("ordType", "Market");

        return params;
    }*/

    /**
     * Checks if short position limit is exceeded
     *
     * @return true if is exceeded, false otherwise
     */
    private boolean short_position_limit_exceeded() {
        if (!Settings.CHECK_POSITION_LIMITS[i])
            return false;
        return e.get_position_size() <= this.minPosition;
    }

    /**
     * Checks if long position limit is exceeded
     *
     * @return true if is exceeded, false otherwise
     */
    private boolean long_position_limit_exceeded() {
        if (!Settings.CHECK_POSITION_LIMITS[i])
            return false;
        return e.get_position_size() >= this.maxPosition;
    }

    /**
     * Returns spread index, based on historical volatility and user settings
     *
     * @return spreadIndex
     */
    private float get_spread_index() {
        TradeBin[] tradeBinData = e.get_tradeBin1m();
        float[] closeArr = new float[tradeBinData.length];
        float midPrice = e.get_mid_price();

        for (int i = 1; i < closeArr.length; i++)
            closeArr[i - 1] = (float) Math.log(tradeBinData[i].getClose() / tradeBinData[i - 1].getClose());

        float currVolIndex = (MathCustom.calculateSD(closeArr) * (float) Math.sqrt(closeArr.length));
        currVolIndex = currVolIndex * (float) Math.sqrt(1f / ((float) closeArr.length / Settings.QUOTE_SPREAD[i]));
        float minimumSpread = get_spread_abs(midPrice + (float) Settings.MIN_SPREAD_TICKS[i] * e.get_tickSize(), midPrice);

        return Math.max(currVolIndex, minimumSpread);
    }

    /**
     * Calculates skew depending on current position size
     *
     * @param spreadIndex - index used in skew formula
     * @return skew
     */
    private float get_position_skew(float spreadIndex) {
        long currPos = e.get_position_size();
        float skew = 0;

        float c = (-1f + (float) Math.pow(2.4, (float) Math.abs(currPos) / (float) this.orderSize / 4f)) * spreadIndex * Settings.QUOTE_SPREAD_FACTOR[i];
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
        float spreadIndex = get_spread_index();
        float tickSize = e.get_tickSize();
        float quoteMidPrice = e.get_mark_price() * (1f + get_position_skew(spreadIndex));
        prices[0] = MathCustom.roundToFraction(quoteMidPrice * (1f - spreadIndex), tickSize);
        prices[1] = MathCustom.roundToFraction(quoteMidPrice * (1f + spreadIndex), tickSize);
        if (Settings.POST_ONLY[i]) {
            prices[0] = Math.min(prices[0], (e.get_ask_price() - tickSize));
            prices[1] = Math.max(prices[1], (e.get_bid_price() + tickSize));
        }
        return prices;
    }

    /**
     * Checks current order spreads to markPrice, and amends them if necessary
     *
     * @param newPrices - new prices calculated based on current skew
     */
    private void check_current_spread(float[] newPrices) throws InterruptedException {
        float fairPrice = e.get_mark_price();
        Order[] topBookOrd = e.get_topBook_orders();

        // check if quoting a wide spread, amend orders if necessary
        if ((topBookOrd[0] != null && topBookOrd[1] != null) && (get_spread_abs(topBookOrd[0].getPrice(), fairPrice) > get_spread_abs(newPrices[0], fairPrice) * Settings.SPREAD_MAINTAIN_RATIO[i]) &&
                (get_spread_abs(topBookOrd[1].getPrice(), fairPrice) > get_spread_abs(newPrices[1], fairPrice) * Settings.SPREAD_MAINTAIN_RATIO[i])) {
            LOGGER.info("Spread wide while quoting both sides, amending both orders.");
            amend_orders_prices(newPrices);
        } else if ((short_position_limit_exceeded() && topBookOrd[0] != null && topBookOrd[1] == null && get_spread_abs(topBookOrd[0].getPrice(), fairPrice) > get_spread_abs(newPrices[0], fairPrice) * Settings.SPREAD_MAINTAIN_RATIO[i]) ||
                (long_position_limit_exceeded() && topBookOrd[1] != null && topBookOrd[0] == null && get_spread_abs(topBookOrd[1].getPrice(), fairPrice) > get_spread_abs(newPrices[1], fairPrice) * Settings.SPREAD_MAINTAIN_RATIO[i])) {
            LOGGER.info("Spread wide while quoting one side, amending order.");
            amend_orders_prices(newPrices);
        }
    }

    /**
     * Amends orders pair with new prices, if orders exist, otherwise does nothing
     *
     * @param newPrices - prices to place the orders using current skew and current position
     */
    private void amend_orders_prices(float[] newPrices) throws InterruptedException {
        JsonArray orders = new JsonArray();
        Order[] topBookOrd = e.get_topBook_orders();

        if (topBookOrd[0] != null && topBookOrd[0].getPrice() != newPrices[0]) {
            JsonObject newBuy = new JsonObject();
            newBuy.addProperty("orderID", topBookOrd[0].getOrderID());
            newBuy.addProperty("price", newPrices[0]);
            orders.add(newBuy);
            LOGGER.info(String.format("Amending %s order price from %f to %f", topBookOrd[0].getSide(), topBookOrd[0].getPrice(), newPrices[0]));
        }
        if (topBookOrd[1] != null && topBookOrd[1].getPrice() != newPrices[1]) {
            JsonObject newSell = new JsonObject();
            newSell.addProperty("orderID", topBookOrd[1].getOrderID());
            newSell.addProperty("price", newPrices[1]);
            orders.add(newSell);
            LOGGER.info(String.format("Amending %s order price from %f to %f", topBookOrd[1].getSide(), topBookOrd[1].getPrice(), newPrices[1]));
        }

        if (!Settings.DRY_RUN && orders.size() > 0) {
            print_status();
            e.amend_order_bulk(orders);
            // interval after http request
            Thread.sleep(API_REST_INTERVAL);
        }
    }

    /**
     * Amends orders with current order quantity, if orders exist, otherwise does nothing
     */
    private void amend_orders_qty() throws InterruptedException {
        JsonArray orders = new JsonArray();
        Order[] topBookOrd = e.get_topBook_orders();

        if (topBookOrd[0] != null && topBookOrd[0].getOrderQty() != this.orderSize) {
            JsonObject newBuy = new JsonObject();
            newBuy.addProperty("orderID", topBookOrd[0].getOrderID());
            newBuy.addProperty("orderQty", this.orderSize);
            orders.add(newBuy);
            LOGGER.info(String.format("Amending %s order quantity from %d to %d", topBookOrd[0].getSide(), topBookOrd[0].getOrderQty(), this.orderSize));
        }
        if (topBookOrd[1] != null && topBookOrd[1].getOrderQty() != this.orderSize) {
            JsonObject newSell = new JsonObject();
            newSell.addProperty("orderID", topBookOrd[1].getOrderID());
            newSell.addProperty("orderQty", this.orderSize);
            orders.add(newSell);
            LOGGER.info(String.format("Amending %s order quantity from %d to %d", topBookOrd[1].getSide(), topBookOrd[1].getOrderQty(), this.orderSize));
        }

        if (!Settings.DRY_RUN && orders.size() > 0) {
            print_status();
            e.amend_order_bulk(orders);
            // interval after http request
            Thread.sleep(API_REST_INTERVAL);
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
        float markPrice = e.get_mark_price();
        Order[] topBookOrd = e.get_topBook_orders();

        if (this.openBuyOrds.removeIf(e::is_buy_order_filled)) {
            LOGGER.info("Buy order filled.");
            fillsCounter++;
        }
        if (this.openSellOrds.removeIf(e::is_sell_order_filled)) {
            LOGGER.info("Sell order filled.");
            fillsCounter++;
        }

        // place new buy order, if no buy order is opened
        if (this.openBuyOrds.size() < 1 && topBookOrd[0] == null && !long_position_limit_exceeded()) {
            JsonObject newBuy = prepare_limit_order(this.orderSize, newPrices[0]);
            orders.add(newBuy);
            LOGGER.info(String.format("Creating buy order of %d contracts at %f (%f)", newBuy.get("orderQty").getAsLong(), newPrices[0], get_spread(newPrices[0], markPrice)));

            // amends current sell order if there is a sell order opened
            if (this.openSellOrds.size() > 0 && topBookOrd[1] != null && topBookOrd[1].getPrice() != newPrices[1]) {
                JsonObject newSell = new JsonObject();
                newSell.addProperty("orderID", topBookOrd[1].getOrderID());
                newSell.addProperty("price", newPrices[1]);
                LOGGER.info(String.format("Amending %s order from %f to %f (%f)", topBookOrd[1].getSide(), topBookOrd[1].getPrice(), newPrices[1], get_spread(newPrices[1], markPrice)));
                if (!Settings.DRY_RUN)
                    e.amend_order(newSell);
            }
        }

        // place new sell order, if no sell order is opened
        if (this.openSellOrds.size() < 1 && topBookOrd[1] == null && !short_position_limit_exceeded()) {
            JsonObject newSell = prepare_limit_order(-this.orderSize, newPrices[1]);
            orders.add(newSell);
            LOGGER.info(String.format("Creating sell order of %d contracts at %f (%f)", newSell.get("orderQty").getAsLong(), newPrices[1], get_spread(newPrices[1], markPrice)));

            // amends current buy order if there is a buy order opened
            if (this.openBuyOrds.size() > 0 && topBookOrd[0] != null && topBookOrd[0].getPrice() != newPrices[0]) {
                JsonObject newBuy = new JsonObject();
                newBuy.addProperty("orderID", topBookOrd[0].getOrderID());
                newBuy.addProperty("price", newPrices[0]);
                LOGGER.info(String.format("Amending %s order from %f to %f (%f)", topBookOrd[0].getSide(), topBookOrd[0].getPrice(), newPrices[0], get_spread(newPrices[0], markPrice)));
                if (!Settings.DRY_RUN)
                    e.amend_order(newBuy);
            }
        }

        if (!Settings.DRY_RUN) {
            if (orders.size() > 0) {
                print_status();
                // makes http request to place orders
                Order[] ordResp = e.place_order_bulk(orders);
                // interval after http request
                Thread.sleep(API_REST_INTERVAL);
                // if order placement had any error
                if (ordResp == null)
                    return;

                for (Order elem : ordResp) {
                    // if order placed with success and still open add it to open orders in local memory
                    if (elem.getOrdStatus() != null && (elem.getOrdStatus().equals("New") || elem.getOrdStatus().equals("PartiallyFilled"))) {
                        if (elem.getSide().equals("Buy"))
                            this.openBuyOrds.add(elem.getOrderID());
                        else if (elem.getSide().equals("Sell"))
                            this.openSellOrds.add(elem.getOrderID());
                    }
                }
            } else
                check_current_spread(newPrices);
        }
    }

    private void sanity_check() throws InterruptedException {
        List<List<Order>> openOrders = e.get_open_orders();

        // number of bids on the order book
        int numBids = openOrders.get(0).size();
        // number of asks on the order book
        int numAsks = openOrders.get(1).size();

        List<String> toCancel = new ArrayList<>(numAsks + numBids);

        // checks how many bids on the order book
        if (numBids > 1) {
            LOGGER.warning(String.format("%d buy orders will be canceled.", numBids - 1));

            // highest buy orderID
            String highestBuyID = e.get_topBook_orders()[0].getOrderID();
            for (Order elem : openOrders.get(0)) {
                String orderID = elem.getOrderID();
                if (!highestBuyID.equals(orderID))
                    toCancel.add(orderID);
            }
        }

        // checks how many asks on the order book
        if (numAsks > 1) {
            LOGGER.warning(String.format("%d ask orders will be canceled.", numAsks - 1));

            // lowest sell orderID
            String lowestSellID = e.get_topBook_orders()[1].getOrderID();
            for (Order elem : openOrders.get(1)) {
                String orderID = elem.getOrderID();
                if (!lowestSellID.equals(orderID))
                    toCancel.add(orderID);
            }
        }

        // http rest request to get open orders
        openOrders = e.rest_get_open_orders();
        List<Order> bids = openOrders.get(0);
        List<Order> asks = openOrders.get(1);

        if (bids.size() < 1 && this.openBuyOrds.size() > 0) {
            LOGGER.info("Removing buy orders from memory. No buy order received from server.");
            this.openBuyOrds.clear();
        } else if (bids.size() > 0 && this.openBuyOrds.size() < 1) {
            LOGGER.info("Adding buy order to memory. Buy order received from server.");
            this.openBuyOrds.add(bids.get(0).getOrderID());
        }

        if (asks.size() < 1 && this.openSellOrds.size() > 0) {
            LOGGER.info("Removing sell orders from memory. No sell order received from server.");
            this.openSellOrds.clear();
        } else if (asks.size() > 1 && this.openSellOrds.size() < 1) {
            LOGGER.info("Adding sell order to memory. Sell order received from server.");
            this.openSellOrds.add(asks.get(0).getOrderID());
        }

        if (toCancel.size() > 0) {
            JsonObject obj = new JsonObject();
            JsonArray params = new JsonArray();

            for (String orderID : toCancel)
                params.add(orderID);
            obj.add("orderID", params);
            e.cancel_orders(obj);
            // interval after http request
            Thread.sleep(API_REST_INTERVAL);
        }

    }

    private void print_status() {
        float spreadIndex = get_spread_index();
        LOGGER.info(String.format("Position: %d", e.get_position_size()));
        LOGGER.info(String.format("Position entry price: %f", e.get_position_entry()));
        LOGGER.info(String.format("Fills in the last 24h: %d", fillsCounter));
        LOGGER.info(String.format("Margin balance: %f", e.get_margin_balance()));
        LOGGER.info(String.format("Margin used: %f%%", e.get_margin_used() * 100f));
        LOGGER.info(String.format("Fair price: %f", e.get_mark_price()));
        LOGGER.info(String.format("Spread index: %f", spreadIndex));
        LOGGER.info(String.format("Skew: %f", get_position_skew(spreadIndex)));
        LOGGER.info("-------------------------------------------------");
    }

    private void run_loop() throws InterruptedException, NotImplementedException {
        while (true) {

            // Only updates / checks orders if websocket connection is up
            if (e.isWebsocketOpen())
                converge_orders();

            long now = System.currentTimeMillis();
            // reset fills counter
            if (now > fillsStamp) {
                LOGGER.info("Resetting fills counter.");
                fillsStamp = fillsStamp + DAY_TO_MILLISECONDS;
                fillsCounter = 0;
            }
            // recalculates order size
            if(e.isWebsocketOpen() && Settings.FLEXIBLE_ORDER_SIZE[i] && e.get_position_size() == 0L && now > calcOrderSizeStamp ) {
                LOGGER.info("Recalculating single order quantities.");
                calcOrderSizeStamp = calcOrderSizeStamp + WEEK_TO_MILLISECONDS;
                calc_pos_max_delta();
                LOGGER.info(String.format("Current single order quantity: %d", this.orderSize));
                amend_orders_qty();
            }
            // data sanity check
            if (now > sanityCheckStamp) {
                sanityCheckStamp = sanityCheckStamp + MINUTE_TO_MILLISECONDS;
                sanity_check();
            }
        }
    }
}

public class MarketMaker {

    private final static Logger LOGGER = Logger.getLogger(MarketMaker.class.getName());

    public static void main(String[] args) throws InterruptedException, NotImplementedException {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %2$s %4$s: %5$s%6$s%n");
        loggingConfig();
        LOGGER.info(String.format("Starting execution in %s with PID: %d", Settings.SYMBOL[0], ProcessHandle.current().pid()));
        new MarketMakerManager(0);
    }

    private static void loggingConfig() {
        Logger log = Logger.getLogger("");
        Handler fileHandler;
        try {
            fileHandler = new FileHandler(String.format("./logs/%s.log", Settings.SYMBOL[0]));
            SimpleFormatter simple = new SimpleFormatter();
            fileHandler.setFormatter(simple);
            //adding Handler for file
            log.addHandler(fileHandler);
        } catch (IOException e) {
            // Do nothing
        }
    }
/*
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
    }*/
}

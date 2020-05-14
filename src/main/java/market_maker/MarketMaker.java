package market_maker;

import bitmex.rest.RestImp;
import bitmex.ws.WsImp;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import utils.SpotPricesTracker;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

class ExchangeInterface {
    private final static Logger LOGGER = Logger.getLogger(ExchangeInterface.class.getName());
    // Funding interval of perpetual swaps on miliseconds
    private final static long FUNDING_INTERVAL = 28800000;

    private RestImp mexRest;
    private WsImp mexWs;
    private String orderIDPrefix;
    private String symbol;
    private Settings settings;
    // class that deals with ticker data on other exchanges
    private SpotPricesTracker spotPrices;
    // array that stores exchanges names that are used as index in our symbol
    private String[] refs;
    // Underlying symbol of contract
    private String underlyingSymbol;
    // if perpetual contract null, otherwise date of expiration
    private String expiry;

    private float[] weights;
    private boolean postOnly;

    public ExchangeInterface(Settings settings) {
        this.settings = settings;
        this.orderIDPrefix = "mmbitmex";
        this.symbol = settings.SYMBOL;
        this.mexRest = new RestImp(true, settings.API_KEY, settings.API_SECRET, orderIDPrefix);
        this.mexWs = new WsImp(true, settings.API_KEY, settings.API_SECRET, symbol);
        this.spotPrices = new SpotPricesTracker(symbol);

        // Initial data

        JsonObject instrument = mexWs.get_instrument().get(0).getAsJsonObject();
        this.expiry = instrument.get("expiry") == null ? null : instrument.get("expiry").getAsString();
        this.underlyingSymbol = instrument.get("underlyingSymbol").getAsString();
        // underlying symbol ( eg. 'XBT=' ) need to convert to ( '.BXBT')
        this.underlyingSymbol = ".B".concat(underlyingSymbol.split("=")[0]);
    }

    public void get_instrument_composite_index() {
        // request to know composition of index on the symbol we are quoting
        JsonArray compIndRes = mexRest.get_instrument_compositeIndex(underlyingSymbol);
        System.out.println(compIndRes);
        List<String> exchangeRefs = new ArrayList<>();
        for(JsonElement elem: compIndRes) {
            String reference = elem.getAsJsonObject().get("reference").getAsString();
            if(reference.equalsIgnoreCase("BMI")) break;
            exchangeRefs.add(reference);
        }
        this.refs = exchangeRefs.toArray(new String[exchangeRefs.size()]);
        spotPrices.addExchanges(this.refs);
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

    public float get_vol_index() {
        return (float) 0.0;
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
        String execInst = this.postOnly ? "ParticipateDoNotInitiate" : "";

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
}

class OrderManager {
    public OrderManager() {

    }
}

public class MarketMaker {

    private final static Logger LOGGER = Logger.getLogger(MarketMaker.class.getName());

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %4$s: %5$s%6$s%n");
        ExchangeInterface e = new ExchangeInterface(new Settings());
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

package bitmex;

import bitmex.rest.RestImp;
import bitmex.settings.Settings;
import bitmex.ws.WsImp;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

class ExchangeInterface {
    private final static Logger LOGGER = Logger.getLogger(ExchangeInterface.class.getName());
    private RestImp rest;
    private WsImp ws;
    private String orderIDPrefix;

    public ExchangeInterface(Settings settings) {
        this.rest = new RestImp(true, settings.API_KEY, settings.API_SECRET, "mm_bitmex_");
        this.ws = new WsImp(true, settings.API_KEY, settings.API_SECRET, "\"instrument:XBTUSD\",\"orderBookL2:XBTUSD\",\"liquidation:XBTUSD\"," +
                "\"order:XBTUSD\",\"position:XBTUSD\",\"execution:XBTUSD\",\"tradeBin1m:XBTUSD\"");
        this.orderIDPrefix = "mm_bitmex_";
        this.ws.waitForData();
    }

    /**
     * Returns last price
     * @return last price
     */
    public float getLastPrice() {
       return this.ws.get_instrument().get(0).getAsJsonObject().get("lastPrice").getAsFloat();
    }

    /**
     * Returns position size
     * @return position size
     */
    public long getPosition() {
        return this.ws.get_position().get(0).getAsJsonObject().get("currentQty").getAsLong();
    }

    /**
     * Get open buy orders
     * @return open buy orders
     */
    public JsonArray get_open_buy_orders() {
        JsonArray ret = new JsonArray();
        JsonArray openOrders = this.ws.get_openOrders(this.orderIDPrefix);
        for(JsonElement elem: openOrders) {
           if( elem.getAsJsonObject().get("side").getAsString().equals("Buy"))
               ret.add(elem);
        }
        return ret;
    }

    /**
     * Get open sell orders
     * @return open sell orders
     */
    public JsonArray get_open_sell_orders() {
        JsonArray ret = new JsonArray();
        JsonArray openOrders = this.ws.get_openOrders(this.orderIDPrefix);
        for(JsonElement elem: openOrders) {
            if( elem.getAsJsonObject().get("side").getAsString().equals("Sell"))
                ret.add(elem);
        }
        return ret;
    }

    /**
     * Get margin used
     * @return margin used
     */
    public long get_margin_used() {
        JsonObject margin = this.ws.get_margin().get(0).getAsJsonObject();
        return margin.get("marginAvailable").getAsLong() / margin.get("marginBalance").getAsLong();
    }

    public float get_vol_index() {
        return (float) 0.0;
    }


}

class OrderManager {
    public OrderManager() {

    }
}

public class market_maker {

    public static void main(String[] args) {

    }
}

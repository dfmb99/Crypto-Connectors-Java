package market_maker;

import bitmex.ws.Ws;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Logger;

public class Main {

    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws InterruptedException, ParseException, IOException {
        Gson g = new Gson();
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %4$s: %5$s%6$s%n");
        // open websocket
        //TESTNET
        //XGN7I-BhV7giM-ihQwo9Rw3F
        //LImmg5mVEHDonA34aniNXwGpsWWYERfZxshUX3ihfXEZ4wwM

        //MAIN
        //swGvEbz7gQG1uAFRMheNby3D
        //0e2uBzGI_A1PpGqPiaY3hxY9nqhHFv4jyAbt38SbP7Q73DHJ

        // bitstamp.WsImp ws = null;
        //new WsImp(null, Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, "XBTUSD");

        JsonObject params = new JsonObject();
        params.addProperty("symbol", "XBTUSD");
        params.addProperty("filter", "{\"ordStatus.isTerminated\": false}");
        params.addProperty("count", Ws.ORDER_MAX_LEN);
        params.addProperty("reverse", true);

        System.out.println(params.get("filter"));
    }

}




package market_maker;

import bitmex.ws.WsImp;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Logger;

public class Main {

    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main2(String[] args) throws InterruptedException, ParseException, IOException {
        Gson g = new Gson();
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %4$s: %5$s%6$s%n");
        // open websocket
        //TESTNET
        //XGN7I-BhV7giM-ihQwo9Rw3F
        //LImmg5mVEHDonA34aniNXwGpsWWYERfZxshUX3ihfXEZ4wwM

        //MAIN
        //swGvEbz7gQG1uAFRMheNby3D
        //0e2uBzGI_A1PpGqPiaY3hxY9nqhHFv4jyAbt38SbP7Q73DHJ

        WsImp ws = null;
         ws = new WsImp(null,false, "swGvEbz7gQG1uAFRMheNby3D",
         "0e2uBzGI_A1PpGqPiaY3hxY9nqhHFv4jyAbt38SbP7Q73DHJ", "XBTM20");


    }
    public static void main(String[] args) {
        JsonObject obj = new JsonObject();
        System.out.println(get_position_skew());
    }

    /**
     * Calculates skew depending on current position size
     *
     * @return skew
     */
    private static float get_position_skew() {
        long currPos = -60;
        float skew = 0;

        float c = (-1f + (float) Math.pow(2.4, (float) Math.abs(currPos) / (float) Settings.ORDER_SIZE / 4f)) * 0.001f * 0.8f;
        if (currPos > 0)
            skew = c * -1f;
        else if (currPos < 0)
            skew = c;

        return skew;
    }
}




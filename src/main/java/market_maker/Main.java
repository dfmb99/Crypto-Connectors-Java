package market_maker;

import bitmex.ws.WsImp;
import com.google.gson.Gson;

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

        WsImp ws = null;
         ws = new WsImp(false, "swGvEbz7gQG1uAFRMheNby3D",
         "0e2uBzGI_A1PpGqPiaY3hxY9nqhHFv4jyAbt38SbP7Q73DHJ", "XBTM20");
        float fairBasisRate = -1f;
         while(true){
             Thread.sleep(100);
             if(ws.get_instrument().get(0).getAsJsonObject().get("fairBasisRate").getAsFloat() != fairBasisRate) {
                 fairBasisRate = ws.get_instrument().get(0).getAsJsonObject().get("fairBasisRate").getAsFloat();
                 System.out.println(ws.get_instrument().get(0).getAsJsonObject().get("timestamp").getAsString() + "     New fair basis rate" + fairBasisRate);
             }
        }
    }


}




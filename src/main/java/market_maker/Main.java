package market_maker;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.ParseException;

public class Main {

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

        /**WsImp ws = null;
        ws = new WsImp(true, "XGN7I-BhV7giM-ihQwo9Rw3F",
                "r0b7mi3r1ioUjrjvii0d0HAp0c2PE7aRVDEPUuhrCdKVwqJu", "\"instrument:XBTUSD\",\"orderBookL2:XBTUSD\",\"liquidation:XBTUSD\"," +
                "\"order:XBTUSD\",\"position:XBTUSD\",\"execution:XBTUSD\",\"tradeBin1m:XBTUSD\"");
*/
        bittrex.WsImp ws = new bittrex.WsImp("USD-BTC");
        while(true) {
            //System.out.println(ws.get_last_price());
        }
    }
}




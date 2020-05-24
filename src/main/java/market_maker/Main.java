package market_maker;

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

        // bitstamp.WsImp ws = null;
        //new WsImp(null, Settings.TESTNET, Settings.API_KEY, Settings.API_SECRET, "XBTUSD");

        ExecThread t = new ExecThread();
        Thread.sleep(10000);
        t.interrupt();


        while(true) {
            System.out.println("MAIN");
            Thread.sleep(1000);
        }
    }
}

class ExecThread extends Thread {
    public ExecThread() {
        this.start();
    }

    @Override
    public void run() {
        while(!this.isInterrupted()) {
            System.out.println("Running algo");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                this.interrupt();
            }
        }
        System.out.println("Cancel orders");
    }
}




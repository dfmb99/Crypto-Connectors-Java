package bitmex;

import bitmex.exceptions.WsError;
import bitmex.ws.WsImp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws InterruptedException, ParseException {

        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %4$s: %5$s%6$s%n");
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            Date date = format.parse("2020-05-07T10:03:20.000Z");
            System.out.println(date.getTime());
      try {
            // open websocket
            //TESTNET
            //
            //LImmg5mVEHDonA34aniNXwGpsWWYERfZxshUX3ihfXEZ4wwM

            //MAIN
            //swGvEbz7gQG1uAFRMheNby3D
            //0e2uBzGI_A1PpGqPiaY3hxY9nqhHFv4jyAbt38SbP7Q73DHJ
            new Thread(() -> {
                WsImp ws = new WsImp(Bitmex.WS_TESTNET, "7ZqTq-r_9eG2kpCwTgpX-VfY",
                        "LImmg5mVEHDonA34aniNXwGpsWWYERfZxshUX3ihfXEZ4wwM", "XBTUSD");
                try {
                    ws.setSubscriptions("\"instrument:XBTUSD\",\"orderBookL2:XBTUSD\",\"liquidation:XBTUSD\"," +
                            "\"order:XBTUSD\"");
                } catch (WsError wsError) {
                    wsError.printStackTrace();
                }
                ws.initConnection();

                while (true) {
                    //System.out.println(ws.getL2Size(9030));
                }
            }).start();
        } catch (Exception ex) {
            System.err.println("InterruptedException exception: " + ex.getMessage());
        }

        /**
        Map<String, JsonArray> data = new ConcurrentHashMap<>();
        data.put("order", new JsonArray());

        new Thread( ()-> {
            while(true) {
            try {
                Thread.sleep(1000);
                JsonArray obj = data.get("order");
                if(obj.size() > 100)
                    System.out.println("Error size of array: " + obj.size());
                System.out.println("Removed element: " + obj.remove(0));

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            }
        }).start();
        long counter = 0;
        while(true) {

            JsonArray obj = data.get("order");
            if(obj.size() > 100)
                System.out.println("Error size of array: " + obj.size());
            if(obj.size() == 100)
                obj.remove(0);
            JsonObject j = new JsonObject();
            j.addProperty(String.valueOf(counter), "asd");
            obj.add(j);
            System.out.println("Inserted element: " + j);
            counter++;
        }
         */
    }
}




package bitmex;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %4$s: %5$s%6$s%n");
        try {
            // open websocket
            //TESTNET
            //7ZqTq-r_9eG2kpCwTgpX-VfY
            //LImmg5mVEHDonA34aniNXwGpsWWYERfZxshUX3ihfXEZ4wwM

            //MAIN
            //swGvEbz7gQG1uAFRMheNby3D
            //0e2uBzGI_A1PpGqPiaY3hxY9nqhHFv4jyAbt38SbP7Q73DHJ
            new Thread(() -> {
                WsImp ws = new WsImp(Bitmex.WS_TESTNET, "7ZqTq-r_9eG2kpCwTgpX-VfY",
                        "LImmg5mVEHDonA34aniNXwGpsWWYERfZxshUX3ihfXEZ4wwM", "XBTUSD");
                ws.setSubscriptions("\"instrument:XBTUSD\",\"orderBookL2:XBTUSD\",\"liquidation:XBTUSD\"," +
                        "\"order:XBTUSD\"");
                //ws.setSubscriptions("");
                ws.initConnection();

                while (true) {
                }
            }).start();
        } catch (Exception ex) {
            System.err.println("InterruptedException exception: " + ex.getMessage());
        }
    }
}




package bitmex;

public class Main {
    public static void main(String[] args) {
            try {
            // open websocket
            new Thread( () ->  {
                WsImp ws = new WsImp(Bitmex.WS_TESTNET, "grGeYloEVIGdb10v66UTKSRW",
                        "olSRpZIc0aoPoMcB7qk50Xa8qnaEUJgBxMaIJBnX5RtpZ4F2", "XBTUSD");
                ws.setSubscriptions("instrument:XBTUSD");
                ws.initConnection();
                while(true);
            }).start();

        } catch (Exception ex) {
            System.err.println("InterruptedException exception: " + ex.getMessage());
        }
    }
}

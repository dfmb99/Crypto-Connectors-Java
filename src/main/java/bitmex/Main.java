package bitmex;

import java.util.logging.Logger;

/**
 * public class Main {
 * <p>
 * public static void main(String[] args) {
 * System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %4$s: %5$s%6$s%n");
 * try {
 * // open websocket
 * new Thread(() -> {
 * WsImp ws = new WsImp(Bitmex.WS_TESTNET, "grGeYloEVIGdb10v66UTKSRW",
 * "olSRpZIc0aoPoMcB7qk50Xa8qnaEUJgBxMaIJBnX5RtpZ4F2", "XBTUSD");
 * ws.setSubscriptions("\"instrument:XBTUSD\",\"orderBookL2:XBTUSD\"");
 * ws.initConnection();
 * while (true) ;
 * }).start();
 * } catch (Exception ex) {
 * System.err.println("InterruptedException exception: " + ex.getMessage());
 * }
 * }
 * }
 */
class MyThread extends Thread {
    private final static Logger LOGGER = Logger.getLogger("");
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(5000);
                LOGGER.warning("Sending ping to server.");
                Thread.sleep(5000);
                LOGGER.warning("Reconnect.");
            } catch (InterruptedException e) {
                interrupt();
            }
        }
        LOGGER.warning("Stopped Running.....");
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT - %4$s: %5$s%6$s%n");
        MyThread thread;
        while (true) {
            thread = new MyThread();
            thread.start();
            Thread.sleep(11000);
            thread.interrupt();
        }
    }
}
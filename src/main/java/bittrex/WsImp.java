package bittrex;

import com.github.ccob.bittrex4j.BittrexExchange;

import java.io.IOException;
import java.util.logging.Logger;

public class WsImp {
    private final static Logger LOGGER = Logger.getLogger(WsImp.class.getName());
    private final static int RETRY_PERIOD = 3000;

    private String symbol;
    private float lastPrice;

    /**
     * Bittrex web socket implementation by https://github.com/CCob/bittrex4j
     *
     * @param symbol
     */
    public WsImp(String symbol) {
        this.symbol = symbol;
        this.lastPrice = -1f;
        new Thread( () -> this.connect() ).start();
        this.waitForData();
    }

    private void connect() {
        try (BittrexExchange bittrexExchange = new BittrexExchange()) {

            bittrexExchange.onUpdateExchangeState(exchangeState -> {
                this.lastPrice = (float) exchangeState.getFills()[0].getPrice();
            });

            bittrexExchange.connectToWebSocket(() -> {
                bittrexExchange.subscribeToExchangeDeltas(this.symbol, null);
            });

            System.in.read();
        } catch (IOException e) {
            // Do nothing
        }
    }

    /**
     * waits for instrument ws data, blocking thread
     */
    private void waitForData() {
        LOGGER.info("Waiting for data.");
        while (this.lastPrice < 0.0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
        LOGGER.info("Data received.");
    }

    public float get_last_price() {
        return lastPrice;
    }
}
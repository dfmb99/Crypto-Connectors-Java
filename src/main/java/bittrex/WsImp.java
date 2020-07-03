package bittrex;

import com.github.ccob.bittrex4j.BittrexExchange;
import com.github.ccob.bittrex4j.dao.Fill;

import java.io.IOException;
import java.util.logging.Logger;

public class WsImp {
    private final static Logger LOGGER = Logger.getLogger(WsImp.class.getName());
    private final static int MAX_LATENCY = 15000;

    private final String symbol;
    private float lastPrice;
    private BittrexExchange bittrexExchange;

    /**
     * Bittrex web socket implementation by https://github.com/CCob/bittrex4j
     *
     * @param symbol - symbol of ticker
     */
    public WsImp(String symbol) {
        this.symbol = symbol;
        this.lastPrice = -1f;
        new Thread(this::connect).start();
        this.waitForData();
    }

    private void connect() {
        try {
            bittrexExchange = new BittrexExchange();
            LOGGER.info("Connected to bittrex websocket.");

            bittrexExchange.onUpdateExchangeState(exchangeState -> {
                LOGGER.fine("Received ticker data.");
                Fill fill = exchangeState.getFills()[0];
                check_latency(fill.getTimeStamp().toEpochSecond() * 1000);
                this.lastPrice = (float) fill.getPrice();
            });

            bittrexExchange.connectToWebSocket(() -> bittrexExchange.subscribeToExchangeDeltas(this.symbol, null));

            System.in.read();
        } catch (IOException e) {
            // Do nothing
        }
    }

    /**
     * Checks latency on a websocket update
     * @param timestamp - epoch stamp in ms
     */
    private void check_latency(long timestamp) {
        long latency = System.currentTimeMillis() - timestamp;
        if( latency > MAX_LATENCY) {
            LOGGER.warning(String.format("Reconnecting to websocket due to high latency of: %d", latency));
            // disconnect from websocket and auto reconnects
            this.bittrexExchange.disconnectFromWebSocket();
        }
    }

    /**
     * waits for instrument ws data, blocking thread
     */
    private void waitForData() {
        while (this.lastPrice < 0.0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    public float get_last_price() {
        return lastPrice;
    }
}
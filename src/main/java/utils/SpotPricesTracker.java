package utils;

import java.util.Arrays;
import java.util.logging.Logger;

public class SpotPricesTracker {
    private final static Logger LOGGER = Logger.getLogger(SpotPricesTracker.class.getName());

    private static final String[] MEX_CONTRACT = {"XBTUSD", "XBTM20"};

    private static final String[] KRAKEN = {"KRAK", "BTC/USD", "BTC/USD"};
    private static final String[] ITBIT = {"ITBT", "XBTUSD", "XBTUSD"};
    private static final String[] BITTREX = {"BTRX", "USD-BTC", "USD-BTC"};
    private static final String[] GEMINI = {"GMNI", "BTCUSD", "BTCUSD"};
    private static final String[] BITSTAMP = {"BSTP", "btcusd", "btcusd"};
    private static final String[] COINBASE = {"GDAX", "BTC-USD", "BTC-USD"};


    private kraken.WsImp kraken;
    private itbit.WsImp itbit;
    private bittrex.WsImp bittrex;
    private gemini.WsImp gemini;
    private bitstamp.WsImp bitstamp;
    private coinbase.WsImp coinbase;
    private String[] refs;
    private final int index;

    /**
     * @param symbol - symbol of bitmex contract that we want to track the indexes
     */
    public SpotPricesTracker(String symbol) {
        index = Arrays.asList(MEX_CONTRACT).indexOf(symbol) + 1;
        if (index < 1) {
            LOGGER.warning(String.format("Symbol not implemented: %s", symbol));
            System.exit(1);
        }
        this.kraken = null;
        this.itbit = null;
        this.bittrex = null;
        this.gemini = null;
        this.bitstamp = null;
        this.coinbase = null;
    }

    /**
     * @param refs - list of exchanges references to track the price
     */
    public void addExchanges(String[] refs) {
        Thread[] myThreads = new Thread[refs.length];
        this.refs = refs;

        for (int i = 0; i < refs.length; i++) {
            if (kraken == null && refs[i].equalsIgnoreCase(KRAKEN[0]))
                myThreads[i] = new Thread(() -> kraken = new kraken.WsImp(KRAKEN[index]));
            else if (itbit == null && refs[i].equalsIgnoreCase(ITBIT[0]))
                myThreads[i] = new Thread(() -> itbit = new itbit.WsImp(ITBIT[index]));
            else if (bittrex == null && refs[i].equalsIgnoreCase(BITTREX[0]))
                myThreads[i] = new Thread(() -> bittrex = new bittrex.WsImp(BITTREX[index]));
            else if (gemini == null && refs[i].equalsIgnoreCase(GEMINI[0]))
                myThreads[i] = new Thread(() -> gemini = new gemini.WsImp(GEMINI[index]));
            else if (bitstamp == null && refs[i].equalsIgnoreCase(BITSTAMP[0]))
                myThreads[i] = new Thread(() -> bitstamp = new bitstamp.WsImp(BITSTAMP[index]));
            else if (coinbase == null && refs[i].equalsIgnoreCase(COINBASE[0]))
                myThreads[i] = new Thread(() -> coinbase = new coinbase.WsImp(COINBASE[index]));
            else {
                LOGGER.warning(String.format("Exchange reference not implemented: %s", refs[i]));
                System.exit(1);
            }
        }

        for (Thread myThread : myThreads) {
            myThread.start();
        }
        for (Thread myThread : myThreads) {
            try {
                myThread.join();
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    /**
     * @return prices - exchange prices in array
     */
    public float[] get_last_price() {
        float[] prices = new float[this.refs.length];
        for (int i = 0; i < refs.length; i++) {
            if (refs[i].equalsIgnoreCase(KRAKEN[0]))
                prices[i] = kraken.get_last_price();
            else if (refs[i].equalsIgnoreCase(ITBIT[0]))
                prices[i] = itbit.get_last_price();
            else if (refs[i].equalsIgnoreCase(BITTREX[0]))
                prices[i] = bittrex.get_last_price();
            else if (refs[i].equalsIgnoreCase(GEMINI[0]))
                prices[i] = gemini.get_last_price();
            else if (refs[i].equalsIgnoreCase(BITSTAMP[0]))
                prices[i] = bitstamp.get_last_price();
            else if (refs[i].equalsIgnoreCase(COINBASE[0]))
                prices[i] = coinbase.get_last_price();
        }
        return prices;
    }

    /**
     * Returns array of exchange references
     * @return array of exchange references
     */
    public String[] get_exchanges_refs() {
        return this.refs;
    }
}

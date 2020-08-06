package binance.rest;

import binance.data.MarkPrice;

public interface Rest {
    //Rest endpoints and path
    String REST_USDT_FUTURES = "https://fapi.binance.com";

    //Server configuration
    int CONNECTION_TIMEOUT = 3000;
    int REPLY_TIMEOUT = 3000;
    int RETRY_PERIOD = 3000;


    /**
     * Returns Mark Price and Funding Rate
     * @param symbol - not mandatory
     * @return
     */
    MarkPrice get_mark_price(String symbol);
}

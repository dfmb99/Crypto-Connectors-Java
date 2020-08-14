package binance.rest;

import binance.data.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public interface Rest {
    //Rest endpoints and path
    String REST_USDT_FUTURES = "https://fapi.binance.com";
    String REST_USDT_FUTURES_TESTNET = "https://testnet.binancefuture.com";

    //Server configuration
    int CONNECTION_TIMEOUT = 3000;
    int REPLY_TIMEOUT = 3000;
    int RETRY_PERIOD = 3000;


    /**
     * Returns Mark Price and Funding Rate
     * @param symbol - symbol
     * @return rest response, null if error
     */
    MarkPrice get_mark_price(@NotNull String symbol);

    /**
     * Returns Kline/candlestick bars for a symbol. Klines are uniquely identified by their open time.
     * @param params - parameters data
     *
     * Name	      Type	  Mandatory	   Description
     * symbol	  STRING    YES
     * interval   ENUM	    YES        '1m' '3m' '5m' '15m' '30m' '1h''2h''4h' '6h' '8h' '12h' '1d' '3d' '1w' '1M'
     * startTime  LONG	    NO
     * endTime	  LONG	    NO
     * limit	  INT	    NO	    Default 500; max 1500.
     * @return rest response, null if error
     */
    JsonArray get_klines(@NotNull JsonObject params);

    /**
     * Send in a new order.
     * @param params - parameters data
     *
     * Name	             Type    Mandatory	 Description
     * symbol	        STRING	   YES
     * side	             ENUM  	   YES
     * positionSide	     ENUM	   NO	     Default BOTH for One-way Mode ; LONG or SHORT for Hedge Mode. It must be sent in Hedge Mode.
     * type	             ENUM	   YES
     * timeInForce	     ENUM	   NO
     * quantity	         DECIMAL   NO	     Cannot be sent with closePosition=true(Close-All)
     * reduceOnly	     STRING	   NO	     "true" or "false". default "false". Cannot be sent in Hedge Mode; cannot be sent with closePosition=true
     * price	         DECIMAL   NO
     * newClientOrderId	 STRING	   NO	      A unique id among open orders. Automatically generated if not sent.
     * stopPrice	     DECIMAL   NO	      Used with STOP/STOP_MARKET or TAKE_PROFIT/TAKE_PROFIT_MARKET orders.
     * closePosition	 STRING	   NO	      true, false；Close-All，used with STOP_MARKET or TAKE_PROFIT_MARKET.
     * activationPrice	 DECIMAL   NO	      Used with TRAILING_STOP_MARKET orders, default as the latest price(supporting different workingType)
     * callbackRate	     DECIMAL   NO	      Used with TRAILING_STOP_MARKET orders, min 0.1, max 5 where 1 for 1%
     * workingType	     ENUM	   NO	      stopPrice triggered by: "MARK_PRICE", "CONTRACT_PRICE". Default "CONTRACT_PRICE"
     * newOrderRespType	 ENUM	   NO	      "ACK", "RESULT", default "ACK"
     * recvWindow	     LONG	   NO
     * timestamp	     LONG	   YES
     *
     * Type	                              Additional mandatory parameters
     * LIMIT	                           timeInForce, quantity, price
     * MARKET	                           quantity
     * STOP/TAKE_PROFIT	                   quantity, price, stopPrice
     * STOP_MARKET/TAKE_PROFIT_MARKET	   stopPrice
     * TRAILING_STOP_MARKET	               callbackRate
     * @return rest response, null if error
     */
    Order place_order(@NotNull JsonObject params);

    /**
     * Send in a new order.
     * @param batchOrders - batchOrders data
     * @return rest response, null if error
     */
    Order[] place_batched_orders(@NotNull JsonArray batchOrders);

    /**
     * Check an order's status.
     * @param params - parameters data
     *
     * Name	                Type	Mandatory
     * symbol	            STRING	   YES
     * orderId	            LONG	   NO
     * origClientOrderId	STRING	   NO
     * recvWindow	        LONG	   NO
     * timestamp	        LONG	   YES
     *
     * Notes:
     * -> Either orderId or origClientOrderId must be sent.
     * @return rest response, null if error
     */
    Order query_order(@NotNull JsonObject params);

    /**
     * Cancel an active order.
     * @param params - parameters data
     *
     * Name	                 Type	Mandatory
     * symbol	            STRING	   YES
     * orderId	             LONG	   NO
     * origClientOrderId	STRING	   NO
     * recvWindow	         LONG	   NO
     * timestamp	         LONG	   YES
     *
     * Notes:
     * -> Either orderId or origClientOrderId must be sent.
     * @return rest response, null if error
     */
    Order cancel_order(@NotNull JsonObject params);

    /**
     * Cancel an active order.
     * @param symbol - symbol to cancel all orders
     * @return rest response, null if error
     */
    Order cancel_all_orders(@NotNull String symbol);

    /**
     * Cancel multiple active orders.
     * @param params - parameters data
     * @return rest response, null if error
     */
    Order[] cancel_multiple_orders(@NotNull JsonObject params);

    /**
     * Cancel all open orders of the specified symbol at the end of the specified countdown.
     * @param symbol - symbol
     * @param countdownTime - countdown time, 1000 for 1 second. 0 to cancel the timer
     * @return rest response, null if error
     */
    Order[] auto_cancel_all_orders(@NotNull String symbol, long countdownTime);

    /**
     * Get all open orders on a symbol. Careful when accessing this with no symbol.
     * @param symbol - symbol
     * @return rest response, null if error
     */
    Order[] query_all_open_order(@NotNull String symbol);

    /**
     * Get futures account balance.
     * @return rest response, null if error
     */
    AccountBalance[] futures_account_balance();

    /**
     * Changes margin type of account.
     * @param - symbol (mandatory)
     * @param - marginType (mandatory) - 'ISOLATED', 'CROSSED'
     * @return rest response, null if error
     */
    boolean change_margin_type(@NotNull String symbol, @NotNull String marginType);

    /**
     * Change user's position mode (Hedge Mode or One-way Mode ) on EVERY symbol
     * @param dualSidePosition - "true": Hedge Mode mode; "false": One-way Mode
     */
    boolean change_position_mode(@NotNull String dualSidePosition);

    /**
     * Get income history.
     * @param params - parameters data
     *
     * Name	       Type	    Mandatory	 Description
     * symbol	   STRING	   NO
     * incomeType  STRING	   NO	     "TRANSFER"，"WELCOME_BONUS", "REALIZED_PNL"，"FUNDING_FEE", "COMMISSION", and "INSURANCE_CLEAR"
     * startTime   LONG	       NO	     Timestamp in ms to get funding from INCLUSIVE.
     * endTime	   LONG	       NO	     Timestamp in ms to get funding until INCLUSIVE.
     * limit	   INT	       NO	     Default 100; max 1000
     * recvWindow  LONG	       NO
     * timestamp   LONG	       YES
     *
     * Notes:
     * -> incomeType is not sent, all kinds of flow will be returned
     * @return rest response, null if error
     */
    Income[] get_income_history(@NotNull JsonObject params);

    /**
     * Start a new user data stream. The stream will close after 60 minutes unless a keepalive is sent. If the account has an active listenKey, that listenKey will be returned and its validity will be extended for 60 minutes.
     * @return ListenKey obj
     */
    ListenKey start_user_stream();

    /**
     * Keepalive a user data stream to prevent a time out. User data streams will close after 60 minutes. It's recommended to send a ping about every 60 minutes.
     * @return empty JsonObject if success, null otherwise
     */
    JsonObject keep_alive_user_stream();

    /**
     * Close out a user data stream.
     * @return empty JsonObject if success, null otherwise
     */
    JsonObject close_user_stream();
}

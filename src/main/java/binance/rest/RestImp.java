package binance.rest;

import binance.data.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static utils.Auth.encode_hmac;

public class RestImp implements Rest {

    private static final Logger logger = LogManager.getLogger(binance.rest.RestImp.class.getName());

    private final Gson g;
    private final String url;
    private final Client client;
    private final String apiKey;
    private final String apiSecret;
    private final String orderIDPrefix;

    public RestImp(String url, String apiKey, String apiSecret, String orderIDPrefix) {
        this.g = new Gson();
        this.url = url;
        this.client = client_configuration();
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.orderIDPrefix = orderIDPrefix;
    }

    /**
     * @param verb     - 'GET', 'POST', 'DELETE', 'PUT'
     * @param endpoint - endpoint on server
     * @param data     - data sent either in url ('GET') or in the body
     * @param signed   - true if request need to be signed, false otherwise
     * @return error message if request could not be retried, or retried request response (both as String)
     */
    private String api_call(String verb, String endpoint, JsonObject data, boolean signed) {
        WebTarget target = client.target(url).path(endpoint);
        for (String name : data.keySet()) {
            String value = String.valueOf(data.get(name));
            if(value.startsWith("["))
                target = target
                        .queryParam(name, URLEncoder.encode(String.valueOf(data.get(name)), StandardCharsets.UTF_8));
            else
                target = target
                        .queryParam(name, URLEncoder.encode(data.get(name).getAsString(), StandardCharsets.UTF_8));
        }

        if (signed) {
            target = target
                    .queryParam("timestamp", System.currentTimeMillis());
            target = target
                    .queryParam("signature", URLEncoder.encode(encode_hmac(this.apiSecret, target.getUri().getRawQuery()), StandardCharsets.UTF_8));
        }

        Invocation.Builder httpReq = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .header("X-MBX-APIKEY", this.apiKey)
                .header("content-type", "application/json;charset=utf-8")
                .header("connection", "keep-alive");

        logger.debug(String.format("Making API request: %s", target.getUri().toString()));

        boolean success = false;
        while (!success) {
            try {
                Response r = httpReq.build(verb).invoke();

                assert r != null;
                int status = r.getStatus();
                success = true;

                if (r.hasEntity()) {
                    if (status == Response.Status.OK.getStatusCode()) {
                        String srvResponse = r.readEntity(String.class);
                        logger.debug(srvResponse);
                        return srvResponse;
                    }else
                        return api_error(status, g.fromJson(r.readEntity(String.class), DefaultMsgAPI.class));
                }
            } catch (ProcessingException pe) { //Error in communication with server
                logger.warn(String.format("Timeout occurred on %s%s. Retrying request...", verb, endpoint));
                try {
                    Thread.sleep(Rest.RETRY_PERIOD); //wait until attempting again.
                } catch (InterruptedException e) {
                    //Nothing to be done here, if this happens we will just retry sooner.
                }
            }
        }
        return null;
    }

    private String api_error(int status, DefaultMsgAPI errObj) {
        logger.warn(String.format("API error, code (%d) %s", errObj.getCode(), errObj.getMsg()));
        try {
            if (status == 429) {
                Thread.sleep(5000);
            } else if (status == 418) {
                Thread.sleep(60000);
            } else if (String.valueOf(status).startsWith("5")) { // error on server side 5xx
                Thread.sleep(3000);
            }
        } catch (InterruptedException e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Returns new order ID with the given prefix
     *
     * @return new order ID
     */
    private String set_new_orderID() {
        return (this.orderIDPrefix + UUID.randomUUID()).substring(0, 28);
    }

    /**
     * Build and configures a Jersey client
     *
     * @return the entry point to the API to execute client requests
     */
    private Client client_configuration() {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, Rest.CONNECTION_TIMEOUT);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, Rest.REPLY_TIMEOUT);
        //Allow changing http headers, before a request
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        return ClientBuilder.newClient(config);
    }

    @Override
    public MarkPrice get_mark_price(String symbol) {
        try {
            JsonObject params = new JsonObject();
            params.addProperty("symbol", symbol);
            return g.fromJson(api_call("GET", "/fapi/v1/premiumIndex", params, false), MarkPrice.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JsonArray get_klines(JsonObject params) {
        try {
            return g.fromJson(api_call("GET", "/fapi/v1/klines", params, false), JsonArray.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Order place_order(JsonObject params) {
        try {
            params.addProperty("newClientOrderId", set_new_orderID());
            return g.fromJson(api_call("POST", "/fapi/v1/order", params, true), Order.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Order[] place_batched_orders(JsonArray batchOrders) {
        try {
            JsonObject params = new JsonObject();
            for(JsonElement e: batchOrders)
                e.getAsJsonObject().addProperty("newClientOrderId", set_new_orderID());
            params.add("batchOrders", batchOrders);
            return g.fromJson(api_call("POST", "/fapi/v1/batchOrders", params, true), Order[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Order query_order(JsonObject params) {
        try {
            return g.fromJson(api_call("GET", "/fapi/v1/order", params, true), Order.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Order cancel_order(JsonObject params) {
        try {
            return g.fromJson(api_call("DELETE", "/fapi/v1/order", params, true), Order.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Order cancel_all_orders(String symbol) {
        try {
            JsonObject params = new JsonObject();
            params.addProperty("symbol", symbol);
            return g.fromJson(api_call("DELETE", "/fapi/v1/allOpenOrders", params, true), Order.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Order[] cancel_multiple_orders(JsonObject params) {
        try {
            return g.fromJson(api_call("DELETE", "/fapi/v1/batchOrders", params, true), Order[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Order[] auto_cancel_all_orders(@NotNull String symbol, long countdownTime) {
        try {
            JsonObject params = new JsonObject();
            params.addProperty("symbol", symbol);
            params.addProperty("countdownTime", countdownTime);
            return g.fromJson(api_call("POST", "/fapi/v1/countdownCancelAll", params, true), Order[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Order[] query_all_open_order(@NotNull String symbol) {
        try {
            JsonObject params = new JsonObject();
            params.addProperty("symbol", symbol);
            return g.fromJson(api_call("GET", "/fapi/v1/openOrders", params, true), Order[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public AccountBalance[] futures_account_balance() {
        try {
            return g.fromJson(api_call("GET", "/fapi/v2/balance", new JsonObject(), true), AccountBalance[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean change_margin_type(@NotNull String symbol, @NotNull String marginType) {
        try {
            JsonObject params = new JsonObject();
            params.addProperty("symbol", symbol);
            params.addProperty("marginType", marginType);

            DefaultMsgAPI resp = g.fromJson(api_call("POST", "/fapi/v1/marginType", params, true), DefaultMsgAPI.class);
            if(resp.getCode() == 200 && resp.getMsg().equalsIgnoreCase("sucess"))
                return true;
            else
                return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean change_position_mode(@NotNull String dualSidePosition) {
        JsonObject params = new JsonObject();
        params.addProperty("dualSidePosition", dualSidePosition);
        DefaultMsgAPI resp = g.fromJson(api_call("POST", "/fapi/v1/positionSide/dual", params, true), DefaultMsgAPI.class);
        if(resp.getCode() == 200 && resp.getMsg().equalsIgnoreCase("sucess"))
            return true;
        else
            return false;
    }

    @Override
    public Income[] get_income_history(@NotNull JsonObject params) {
        try {
            return g.fromJson(api_call("GET", "/fapi/v1/income", params, true), Income[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ListenKey start_user_stream() {
        try {
            return g.fromJson(api_call("POST", "/fapi/v1/listenKey", new JsonObject(), true), ListenKey.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JsonObject keep_alive_user_stream() {
        try {
            return g.fromJson(api_call("PUT", "/fapi/v1/listenKey", new JsonObject(), true), JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JsonObject close_user_stream() {
        try {
            return g.fromJson(api_call("DELETE", "/fapi/v1/listenKey", new JsonObject(), true), JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

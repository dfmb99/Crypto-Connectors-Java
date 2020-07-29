package bitmex.rest;

import bitmex.data.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.UuidUtil;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import utils.Auth;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RestImp implements Rest {

    private static final Logger logger = LogManager.getLogger(RestImp.class.getName());

    private final Gson g;
    private final Client client;
    private final String url;
    private final String apiKey;
    private final String apiSecret;
    private final String orderIDPrefix;

    /**
     * Implementation to connect to the Bitmex Rest API, see more at https://www.bitmex.com/api/explorer/
     *
     * @param testnet       - true if we want to connect to testnet, false otherwise
     * @param apiKey        - apiKey of client
     * @param apiSecret     - apiSecret of client
     * @param orderIDPrefix - every order placed will start with this ID (max 8 characters)
     */
    public RestImp(boolean testnet, String apiKey, String apiSecret, String orderIDPrefix) {
        this.g = new Gson();
        if (testnet)
            this.url = Rest.REST_TESTNET;
        else
            this.url = Rest.REST_MAINNET;
        this.client = client_configuration();
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.orderIDPrefix = orderIDPrefix;
    }

    /**
     * @param verb     - 'GET', 'POST', 'DELETE', 'PUT'
     * @param endpoint - endpoint on server
     * @param data     - data sent either in url ('GET') or in the body
     * @return error message if request could not be retried, or retried request response (both as String)
     */
    public String api_call(String verb, String endpoint, JsonObject data) {
        WebTarget target = client.target(url).path(Rest.API_PATH + endpoint);
        if (verb.equalsIgnoreCase("GET")) {
            for (String name : data.keySet()) {
                target = target
                        .queryParam(name, URLEncoder.encode(data.get(name).getAsString(), StandardCharsets.UTF_8));
            }
        }

        Invocation.Builder httpReq = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .header("content-type", "application/json; charset=utf-8")
                .header("connection", "keep-alive");

        long expires = Auth.generate_expires();
        URI uri = target.getUri();
        String sigData = String.format("%s%s%s%s%s", verb, uri.getPath() == null ? "" : uri.getPath(),
                uri.getQuery() == null ? "" : "?" + uri.toString().split("\\?")[1], expires, verb.equalsIgnoreCase(
                        "GET") ? "" :
                        data.toString());
        String signature = Auth.encode_hmac(apiSecret, sigData);
        httpReq = httpReq
                .header("api-expires", expires)
                .header("api-key", apiKey)
                .header("api-signature", signature);

        logger.debug(String.format("Making API request: %s", uri.toString()));
        logger.debug(String.format("API Request data: %s", data.toString()));

        boolean success = false;
        while (!success) {
            try {
                Response r = null;
                if (verb.equalsIgnoreCase("GET"))
                    r = httpReq.get();
                else if (verb.equalsIgnoreCase("POST"))
                    r = httpReq.post(Entity.entity(data.toString(), MediaType.APPLICATION_JSON));
                else if (verb.equalsIgnoreCase("PUT"))
                    r = httpReq.put(Entity.entity(data.toString(), MediaType.APPLICATION_JSON));
                else if (verb.equalsIgnoreCase("DELETE"))
                    r = httpReq.build("DELETE", Entity.entity(data.toString(), MediaType.APPLICATION_JSON)).invoke();

                assert r != null;
                int status = r.getStatus();
                success = true;

                if (r.hasEntity()) {
                    String srvRes = r.readEntity(String.class);
                    logger.debug(String.format("API request response (%d): %s", status, srvRes));
                    if(status == Response.Status.OK.getStatusCode())
                        return srvRes;
                    else
                        return api_error(status, verb, endpoint, data, srvRes, r.getHeaders());
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
        //Property to allow to post body data in a 'DELETE' request, otherwise an exception is thrown
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        //Suppress warnings for payloads with DELETE calls:
        java.util.logging.Logger.getLogger("org.glassfish.jersey.client").setLevel(java.util.logging.Level.SEVERE);
        //Allow changing http headers, before a request
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        return ClientBuilder.newClient(config);
    }

    /**
     * Deals with an api call which response was non ok (~200)
     *
     * @param status   - http response code
     * @param verb     - 'GET', 'POST', 'DELETE', 'PUT'
     * @param endpoint - endpoint on server
     * @param data     - data sent either in url ('GET') or in the body
     * @param response - response as string received by the server
     * @param headers  - headers of http request
     * @return response of server,  null if error
     */
    private String api_error(int status, String verb, String endpoint, JsonObject data, String response,
                             MultivaluedMap<String, Object> headers) {

        // converts error response to json element
        JsonElement obj = g.fromJson(response, JsonElement.class);
        if (!obj.isJsonObject() || !obj.getAsJsonObject().has("error"))
            return null;

        // gets error element on json
        JsonObject errorObj = obj.getAsJsonObject().get("error").getAsJsonObject();

        // formats error message
        String errLog = String.format("(%d) error on request: %s  Name: %s  Message: %s", status,
                verb + endpoint, errorObj.get("name").toString(),
                errorObj.get("message").toString());

        //Checks response codes
        if (status == 401 || status == 403) {
            // Authentication error, forbidden
            logger.fatal(errLog);
            System.exit(1);
        } else if (status == 400) {
            //Parameter error
            logger.error(errLog);
            sleep(2000); //waits 2000ms
            return response;
        } else if (status == 404) {
            logger.error(errLog);
            sleep(2000); //waits 2000ms until attempting again.
            //Order not found
            if (verb.equalsIgnoreCase("DELETE"))
                return response;
            return api_call(verb, endpoint, data);
        } else if (status == 429) {
            logger.error(errLog);
            long rateLimitReset = Long.parseLong(headers.get("x-ratelimit-reset").get(0).toString());
            long toSleep = rateLimitReset * 1000 - System.currentTimeMillis();
            logger.warn(String.format("Rate-limit will reset at: %d , sleeping for %d ms", rateLimitReset, toSleep));
            sleep(toSleep); //waits until attempting again.
            return api_call(verb, endpoint, data);
        } else if (status == 503) {
            logger.error(errLog);
            sleep(3000); //waits 3000ms until attempting again.
            return api_call(verb, endpoint, data);
        }
        logger.error("Unhandled error response. \n " + response);
        return null;
    }

    /**
     * Sleeps code execution for x ms
     *
     * @param ms - ms amount to sleep
     */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms); //wait ms until attempting again.
        } catch (InterruptedException e) {
            //Nothing to be done here, if this happens we will just retry sooner.
        }
    }

    /**
     * Returns new order ID with the given prefix
     *
     * @return new order ID
     */
    private String setNewOrderID() {
        return (orderIDPrefix + UuidUtil.getTimeBasedUuid().toString()).substring(0, 36);
    }

    @Override
    public Instrument get_instrument(String symbol) {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", symbol);
        try {
            return g.fromJson(api_call("GET", "/instrument", params), Instrument[].class)[0];
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Order[] get_order(JsonObject data) {
        try {
            Order[] response = g.fromJson(api_call("GET", "/order", data), Order[].class);

            // only returns orders that start with the orderIDPrefix
            List<Order> toRet = new ArrayList<>(response.length);
            for (Order v : response) {
                if (v.getClOrdID() != null && v.getClOrdID().startsWith(this.orderIDPrefix))
                    toRet.add(v);
            }

            return toRet.toArray(new Order[0]);
        } catch (Exception e) {
            return new Order[0];
        }
    }

    @Override
    public Order put_order(JsonObject data) {
        try {
            return g.fromJson(api_call("PUT", "/order", data), Order.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Order post_order(JsonObject data) {
        try {
            //Adds cl0rdID
            data.addProperty("clOrdID", setNewOrderID());

            return g.fromJson(api_call("POST", "/order", data), Order.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Order[] del_order(JsonObject data) {
        try {
            return g.fromJson(api_call("DELETE", "/order", data), Order[].class);
        } catch (Exception e) {
            return new Order[0];
        }
    }

    @Override
    public Order[] del_order_all(JsonObject data) {
        try {
            return g.fromJson(api_call("DELETE", "/order/all", data), Order[].class);
        } catch (Exception e) {
            return new Order[0];
        }
    }

    @Override
    public Order[] put_order_bulk(JsonObject data) {
        try {
            return g.fromJson(api_call("PUT", "/order/bulk", data), Order[].class);
        } catch (Exception e) {
            return new Order[0];
        }
    }

    @Override
    public Order[] post_order_bulk(JsonObject data) {
        try {
            //Adds cl0rdID
            JsonArray orders = data.get("orders").getAsJsonArray();
            for (JsonElement e : orders)
                e.getAsJsonObject().addProperty("clOrdID", setNewOrderID());

            return g.fromJson(api_call("POST", "/order/bulk", data), Order[].class);
        } catch (Exception e) {
            return new Order[0];
        }
    }

    @Override
    public Order post_order_cancelAllAfter(JsonObject data) {
        try {
            return g.fromJson(api_call("POST", "/order/cancelAllAfter", data), Order.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Position get_position(JsonObject data) {
        try {
            return g.fromJson(api_call("GET", "/position", data), Position.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public TradeBin[] get_trade_bucketed(JsonObject data) {
        try {
            return g.fromJson(api_call("GET", "/trade/bucketed", data), TradeBin[].class);
        } catch (Exception e) {
            return new TradeBin[0];
        }
    }

    @Override
    public UserMargin get_user_margin() {
        try {
            // XBt only collateral
            JsonObject data = g.fromJson("{'currency': 'XBt'}", JsonObject.class);

            return g.fromJson(api_call("GET", "/user/margin", data), UserMargin.class);
        } catch (Exception e) {
            return null;
        }
    }
}

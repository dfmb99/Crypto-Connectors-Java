package bitmex.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import utils.Auth;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

public class RestImp implements Rest {

    private final static Logger LOGGER = Logger.getLogger(Rest.class.getName());


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
        if (orderIDPrefix.length() > 8) {
            LOGGER.severe("orderIDPrefix max length is 8.");
            System.exit(1);
        }
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

        /** Validates data
         * Original: {"orderID":"[\"88dd843b-80a5-afcc-d8f4-5985f4c0f734\", \"7f4788f4-af46-a8e9-5304-d8df5a1f66d3\"]"}
         * After validation: {"orderID": ["88dd843b-80a5-afcc-d8f4-5985f4c0f734", "7f4788f4-af46-a8e9-5304-d8df5a1f66d3"]
         */
        String validDataStr = data.toString().replace("\"[", "[").replace("]\"","]").replace("\\", "");

        long expires = Auth.generate_expires();
        URI uri = target.getUri();
        String sigData = String.format("%s%s%s%s%s", verb, uri.getPath() == null ? "" : uri.getPath(),
                uri.getQuery() == null ? "" : "?" + uri.toString().split("\\?")[1], expires, verb.equalsIgnoreCase(
                        "GET") ? "" :
                        validDataStr);
        String signature = Auth.encode_hmac(apiSecret, sigData);
        httpReq = httpReq
                .header("api-expires", expires)
                .header("api-key", apiKey)
                .header("api-signature", signature);

        boolean success = false;
        while (!success) {
            try {
                Response r = null;
                if (verb.equalsIgnoreCase("GET"))
                    r = httpReq.get();
                else if (verb.equalsIgnoreCase("POST"))
                    r = httpReq.post(Entity.entity(validDataStr, MediaType.APPLICATION_JSON));
                else if (verb.equalsIgnoreCase("PUT"))
                    r = httpReq.put(Entity.entity(validDataStr, MediaType.APPLICATION_JSON));
                else if (verb.equalsIgnoreCase("DELETE"))
                    r = httpReq.build("DELETE", Entity.entity(validDataStr, MediaType.APPLICATION_JSON)).invoke();

                assert r != null;
                int status = r.getStatus();
                success = true;

                if (status == Response.Status.OK.getStatusCode() && r.hasEntity())
                    return r.readEntity(String.class);
                else if (r.hasEntity())
                    return api_error(status, verb, endpoint, data, r.readEntity(String.class), r.getHeaders());

            } catch (ProcessingException pe) { //Error in communication with server
                LOGGER.info("Timeout occurred.");
                try {
                    Thread.sleep(Rest.RETRY_PERIOD); //wait until attempting again.
                } catch (InterruptedException e) {
                    //Nothing to be done here, if this happens we will just retry sooner.
                }
                LOGGER.info("Retrying to execute request.");
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
     */
    private String api_error(int status, String verb, String endpoint, JsonObject data, String response,
                             MultivaluedMap<String, Object> headers) {
        JsonReader reader = new JsonReader(new StringReader(response));
        reader.setLenient(true);
        JsonObject errorObj = (JsonObject) JsonParser.parseReader(reader).getAsJsonObject().get("error");
        String errLog = String.format("(%d) error on request: %s  Name: %s  Message: %s", status,
                verb + endpoint, errorObj.get("name").toString(),
                errorObj.get("message").toString());
        JsonArray errArr = new JsonArray();
        errArr.add(errorObj);

        if(status == 401 || status == 403) {
            // Authentication error, forbidden
            LOGGER.severe(errLog);
            System.exit(1);
        } else if (status == 400) {
            //Parameter error
            LOGGER.warning(errLog);
            sleep(3000); //waits 3000ms
            // same methods are expecting JsonObject and others expect a JsonArray
            if(((verb.equals("PUT") || verb.equals("POST")) && endpoint.equals("/order")) || endpoint.equals("/order/cancelAllAfter") || endpoint.equals("/user/margin"))
                return errorObj.toString();
            return errArr.toString();
        } else if (status == 404) {
            LOGGER.warning(errLog);
            sleep(3000); //waits 3000ms until attempting again.
            //Order not found
            if (verb.equalsIgnoreCase("DELETE"))
                return errArr.toString();
            return api_call(verb, endpoint, data);
        } else if (status == 429) {
            LOGGER.warning(errLog);
            System.currentTimeMillis();
            long rateLimitReset = Long.parseLong(headers.get("x-ratelimit-reset").get(0).toString());
            //Seconds to sleep
            long toSleep = rateLimitReset * 1000 - System.currentTimeMillis();
            LOGGER.warning(String.format("Ratelimit will reset at: %d , sleeping for %d ms", rateLimitReset, toSleep));
            sleep(toSleep); //waits until attempting again.
            return api_call(verb, endpoint, data);
        } else if (status == 503) {
            LOGGER.warning(errLog);
            sleep(3000); //waits 3000ms until attempting again.
            return api_call(verb, endpoint, data);
        }
        LOGGER.warning("Unhandled error. \n " + errLog);
        return errArr.toString();
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
        return orderIDPrefix + Base64.getEncoder().encodeToString((UUID.randomUUID().toString()).getBytes()).substring(0, 28);
    }

    @Override
    public JsonArray get_execution(JsonObject data) {
        return JsonParser.parseString(api_call("GET", "/execution", data)).getAsJsonArray();
    }

    @Override
    public JsonArray get_execution_tradeHistory(JsonObject data) {
        return JsonParser.parseString(api_call("GET", "/execution/tradeHistory", data)).getAsJsonArray();
    }

    @Override
    public JsonArray get_instrument(String symbol) {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", symbol);
        return JsonParser.parseString(api_call("GET", "/instrument", params)).getAsJsonArray();
    }

    @Override
    public JsonArray get_instrument_compositeIndex(String compIndex) {
        JsonObject params = new JsonObject();
        params.addProperty("symbol", compIndex);
        params.addProperty("count", 50);
        params.addProperty("reverse", true);
        return JsonParser.parseString(api_call("GET", "/instrument/compositeIndex", params)).getAsJsonArray();
    }

    @Override
    public JsonArray get_order(JsonObject data) {
        JsonArray response = new JsonArray();
        JsonArray arr = JsonParser.parseString(api_call("GET", "/order", data)).getAsJsonArray();
        for(JsonElement elem: arr) {
            if( elem.getAsJsonObject().has("clOrdID") && elem.getAsJsonObject().get("clOrdID").getAsString().startsWith(this.orderIDPrefix) )
                response.add(elem);
        }
        return response;
    }

    @Override
    public JsonObject put_order(JsonObject data) {
        return JsonParser.parseString(api_call("PUT", "/order", data)).getAsJsonObject();
    }

    @Override
    public JsonObject post_order(JsonObject data) {
        //Adds cl0rdID property on order
        data.addProperty("clOrdID", setNewOrderID());
        return JsonParser.parseString(api_call("POST", "/order", data)).getAsJsonObject();
    }

    @Override
    public JsonArray del_order(JsonObject data) {
        return JsonParser.parseString(api_call("DELETE", "/order", data)).getAsJsonArray();
    }

    @Override
    public JsonArray del_order_all(JsonObject data) {
        return JsonParser.parseString(api_call("DELETE", "/order/all", data)).getAsJsonArray();
    }

    @Override
    public JsonArray put_order_bulk(JsonObject data) {
        return JsonParser.parseString(api_call("PUT", "/order/bulk", data)).getAsJsonArray();
    }

    @Override
    public JsonArray post_order_bulk(JsonObject data) {
        //Adds cl0rdID property on each order
        JsonArray orders = data.get("orders").getAsJsonArray();
        for (JsonElement e : orders)
            e.getAsJsonObject().addProperty("clOrdID", setNewOrderID());
        return JsonParser.parseString(api_call("POST", "/order/bulk", data)).getAsJsonArray();
    }

    @Override
    public JsonObject post_order_cancelAllAfter(JsonObject data) {
        return JsonParser.parseString(api_call("POST", "/order/cancelAllAfter", data)).getAsJsonObject();
    }

    @Override
    public JsonArray get_position(JsonObject data) {
        return JsonParser.parseString(api_call("GET", "/position", data)).getAsJsonArray();
    }

    @Override
    public JsonArray get_trade_bucketed(JsonObject data) {
        return JsonParser.parseString(api_call("GET", "/trade/bucketed", data)).getAsJsonArray();
    }

    @Override
    public JsonObject get_user_margin() {
        //BitMex only allows BTC as margin
        JsonObject data = JsonParser.parseString("{'currency': 'XBt'}").getAsJsonObject();
        return JsonParser.parseString(api_call("GET", "/user/margin", data)).getAsJsonObject();
    }

    @Override
    public JsonArray get_user_walletHistory(JsonObject data) {
        return JsonParser.parseString(api_call("GET", "/user/walletHistory", data)).getAsJsonArray();
    }

    @Override
    public JsonArray get_user_quoteFillRatio() {
        return JsonParser.parseString(api_call("GET", "/user/quoteFillRatio", new JsonObject())).getAsJsonArray();
    }
}

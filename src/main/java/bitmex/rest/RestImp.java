package bitmex.rest;

import exceptions.ApiConnectionException;
import exceptions.ApiErrorException;
import utils.Auth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
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
     * @param testnet   - true if we want to connect to testnet, false otherwise
     * @param apiKey    - apiKey of client
     * @param apiSecret - apiSecret of client
     * @param orderIDPrefix - every order placed will start with this ID (max 8 characters)
     */
    public RestImp(boolean testnet, String apiKey, String apiSecret, String orderIDPrefix) {
        if(orderIDPrefix.length() > 8) {
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
    public String api_call(String verb, String endpoint, JsonObject data) throws ApiErrorException {
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


                if (r == null) {
                    LOGGER.severe("No response from server.");
                    throw new ApiConnectionException();
                }

                int status = r.getStatus();
                success = true;

                if (status == Response.Status.OK.getStatusCode() && r.hasEntity())
                    return r.readEntity(String.class);
                else if (r.hasEntity())
                    api_error(status, verb, endpoint, data, r.readEntity(String.class), r.getHeaders());

            } catch (ProcessingException pe) { //Error in communication with server
                LOGGER.info("Timeout occurred.");
                try {
                    Thread.sleep(Rest.RETRY_PERIOD); //wait until attempting again.
                } catch (InterruptedException e) {
                    //Nothing to be done here, if this happens we will just retry sooner.
                }
                LOGGER.info("Retrying to execute request.");
            } catch (ApiConnectionException e) { //Unhandled error after api request
                LOGGER.severe(e.getMessage());
                System.exit(1);
            }
        }
        throw new ApiErrorException("Connection Error.");
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
     * @throws ApiErrorException - in case of error with api
     */
    private void api_error(int status, String verb, String endpoint, JsonObject data, String response,
                           MultivaluedMap<String, Object> headers) throws ApiErrorException {
        JsonObject errorObj = (JsonObject) JsonParser.parseString(response).getAsJsonObject().get("error");
        String errName = errorObj.get("name").toString();
        String errMsg = errorObj.get("message").toString();
        String errLog = String.format("(%d) error on request: %s \n Name: %s \n Message: %s", status,
                verb + endpoint, errName,
                errMsg);
        if (status == 400 || status == 401 || status == 403) {
            //Parameter error, Unauthorized or Forbidden
            LOGGER.severe(errLog);
            throw new ApiErrorException(errMsg);
        } else if (status == 404) {
            LOGGER.warning(errLog);
            //Order not found
            if (verb.equalsIgnoreCase("DELETE")) {
                throw new ApiErrorException(errMsg);
            }
            sleep(3000); //waits 3000ms until attempting again.
            api_call(verb, endpoint, data);
            return;
        } else if (status == 429) {
            LOGGER.warning(errLog);
            System.currentTimeMillis();
            long rateLimitReset = (Long) headers.get("x-ratelimit-reset").get(0);
            //Seconds to sleep
            long toSleep = rateLimitReset * 1000 - System.currentTimeMillis();
            LOGGER.warning(String.format("Ratelimit will reset at: %d , sleeping for %d ms", rateLimitReset, toSleep));
            sleep(toSleep); //waits until attempting again.
            api_call(verb, endpoint, data);
            return;
        } else if (status == 503) {
            LOGGER.warning(errLog);
            sleep(3000); //waits 3000ms until attempting again.
            api_call(verb, endpoint, data);
            return;
        }
        LOGGER.severe("Unhandled error. \n " + errLog);
        throw new ApiErrorException(errLog);
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
     * @return new order ID
     */
    private String setNewOrderID() {
        return orderIDPrefix + Base64.getEncoder().encodeToString((UUID.randomUUID().toString()).getBytes()).substring(0,28);
    }

    @Override
    public JsonArray get_execution(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("GET", "/execution", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray get_execution_tradeHistory(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("GET", "/execution/tradeHistory", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray get_instrument(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("GET", "/instrument", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray get_order(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("GET", "/order", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonObject put_order(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("PUT", "/order", data)).getAsJsonObject();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonObject post_order(JsonObject data) {
        try {
            //Adds cl0rdID property on order
            data.addProperty("clOrdID", setNewOrderID());
            return JsonParser.parseString(api_call("POST", "/order", data)).getAsJsonObject();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray del_order(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("DELETE", "/order", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray del_order_all(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("DELETE", "/order/all", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray put_order_bulk(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("PUT", "/order/bulk", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray post_order_bulk(JsonObject data) {
        try {
            //Adds cl0rdID property on each order
            JsonArray orders = data.get("orders").getAsJsonArray();
            for (JsonElement e : orders) {
                e.getAsJsonObject().addProperty("clOrdID", setNewOrderID());
            }
            return JsonParser.parseString(api_call("POST", "/order/bulk", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonObject post_order_cancelAllAfter(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("POST", "/order/cancelAllAfter", data)).getAsJsonObject();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray get_position(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("GET", "/position", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray get_trade_bucketed(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("GET", "/trade/bucketed", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonObject get_user_margin() {
        try {
            //BitMex only allows BTC as margin
            JsonObject data = JsonParser.parseString("{'currency': 'XBt'}").getAsJsonObject();
            return JsonParser.parseString(api_call("GET", "/user/margin", data)).getAsJsonObject();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray get_user_walletHistory(JsonObject data) {
        try {
            return JsonParser.parseString(api_call("GET", "/user/walletHistory", data)).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    @Override
    public JsonArray get_user_quoteFillRatio() {
        try {
            return JsonParser.parseString(api_call("GET", "/user/quoteFillRatio", new JsonObject())).getAsJsonArray();
        } catch (ApiErrorException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }
}

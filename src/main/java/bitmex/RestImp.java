package bitmex;

import bitmex.Exceptions.UnhandledErrorException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Hex;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.logging.Logger;

import static org.apache.commons.codec.digest.HmacAlgorithms.HMAC_SHA_256;

public class RestImp implements Rest {

    private final static Logger LOGGER = Logger.getLogger(Rest.class.getName());
    private final Client client;
    private final String apiKey;
    private final String apiSecret;
    private final boolean auth;

    /**
     * Implementation to connect to the Bitmex Rest API, see more at https://www.bitmex.com/api/explorer/
     *
     * @param apiKey    - apiKey of client, "" otherwise
     * @param apiSecret - apiSecret of client, "" otherwise
     */
    public RestImp(String apiKey, String apiSecret) {
        client = client_configuration();
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        auth = !apiKey.equals("") && !apiSecret.equals("");
    }

    /**
     * @param verb     - 'GET', 'POST', 'DELETE', 'PUT'
     * @param endpoint - endpoint on server
     * @param data     - data sent either in url ('GET') or in the body
     * @return error message if request could not be retried, or retried request response (both as String)
     */
    public String api_call(String verb, String endpoint, JsonObject data) {
        WebTarget target = client.target(Rest.url).path(Rest.apiPath + endpoint);
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

        if (auth) {
            String expires = String.valueOf(Instant.now().getEpochSecond() + 3600);
            URI uri = target.getUri();
            String sigData = String.format("%s%s%s%s%s", verb, uri.getPath() == null ? "" : uri.getPath(),
                    uri.getQuery() == null ? "" : "?" + uri.toString().split("\\?")[1], expires, verb.equalsIgnoreCase(
                            "GET") ? "" :
                            data.toString());
            String signature = encode_hmac(apiSecret, sigData);
            httpReq = httpReq
                    .header("api-expires", expires)
                    .header("api-key", apiKey)
                    .header("api-signature", signature);
        }

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
                    return "";
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
            } catch (UnhandledErrorException e) { // Unhandled error after api request
                LOGGER.severe(e.getMessage());
                System.exit(1);
            }
        }
        LOGGER.severe("Connection Error.");
        return "";
    }

    /**
     * Build and configures a Jersey client
     *
     * @return the entry point to the API to execute client requests
     */
    private Client client_configuration() {
        ClientConfig config = new ClientConfig();
        // how much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, Rest.CONNECTION_TIMEOUT);
        // how much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, Rest.REPLY_TIMEOUT);
        // property to allow to post body data in a 'DELETE' request, otherwise an exception is thrown
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        // suppress warnings for payloads with DELETE calls:
        java.util.logging.Logger.getLogger("org.glassfish.jersey.client").setLevel(java.util.logging.Level.SEVERE);
        // allow changing http headers, before a request
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        return ClientBuilder.newClient(config);
    }

    /**
     * Builds a hmac signature given a key and data
     *
     * @param key  - key to be used in the hmac signature
     * @param data - data to be used in the hmac signature
     * @return result of the hmac signature encryption as String
     */
    public static String encode_hmac(String key, String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance(HMAC_SHA_256.getName());
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256.getName());
            sha256_HMAC.init(secret_key);
            return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.severe(e.getMessage());
            System.exit(1);
            return null;
        }
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
     * @return error message if request could not be retried, or retried request response (both as String)
     * @throws UnhandledErrorException - in case of error could not been handled
     */
    private String api_error(int status, String verb, String endpoint, JsonObject data, String response,
                             MultivaluedMap<String, Object> headers) throws UnhandledErrorException {
        JsonObject errorObj = (JsonObject) JsonParser.parseString(response).getAsJsonObject().get("error");
        String errName = errorObj.get("name").toString();
        String errMsg = errorObj.get("message").toString();
        String errLog = String.format("(%d) error on request: %s \n Name: %s \n Message: %s", status,
                verb + endpoint, errName,
                errMsg);
        if (status == 400 || status == 401 || status == 403) {
            // Parameter error, Unauthorized or Forbidden
            LOGGER.severe(errLog);
            return errMsg;
        } else if (status == 404) {
            LOGGER.warning(errLog);
            // order not found
            if (verb.equalsIgnoreCase("DELETE")) {
                return errMsg;
            }
            sleep(3000); // waits 3000ms until attempting again.
            return api_call(verb, endpoint, data);
        } else if (status == 429) {
            LOGGER.warning(errLog);
            System.currentTimeMillis();
            long rateLimitReset = (Long) headers.get("x-ratelimit-reset").get(0);
            // seconds to sleep
            long toSleep = rateLimitReset * 1000 - System.currentTimeMillis();
            LOGGER.warning(String.format("Ratelimit will reset at: %d , sleeping for %d ms", rateLimitReset, toSleep));
            sleep(toSleep); // waits until attempting again.
            return api_call(verb, endpoint, data);
        } else if (status == 503) {
            LOGGER.warning(errLog);
            sleep(3000); // waits 3000ms until attempting again.
            return api_call(verb, endpoint, data);
        }
        LOGGER.severe("Unhandled error. \n " + errLog);
        throw new UnhandledErrorException();
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

    @Override
    public String get_execution(JsonObject data) {
        return null;
    }

    @Override
    public String get_instrument(JsonObject data) {
        return api_call("GET", "/instrument", data);
    }

    @Override
    public String get_order(JsonObject data) {
        return api_call("GET", "/order", data);
    }

    @Override
    public String put_order(JsonObject data) {
        return null;
    }

    @Override
    public String post_order(JsonObject data) {
        return null;
    }

    @Override
    public String del_order(JsonObject data) {
        return api_call("DELETE", "/order", data);
    }

    @Override
    public String del_order_all(JsonObject data) {
        return null;
    }

    @Override
    public String put_order_bulk(JsonObject data) {
        return null;
    }

    @Override
    public String post_order_bulk(JsonObject data) {
        return null;
    }

    @Override
    public String post_order_cancelAllAfter(JsonObject data) {
        return null;
    }

    @Override
    public String get_position(JsonObject data) {
        return null;
    }

    @Override
    public String get_trade_bucketed(JsonObject data) {
        return null;
    }

    @Override
    public String get_user_margin(JsonObject data) {
        return null;
    }

    @Override
    public String get_user_walletHistory(JsonObject data) {
        return null;
    }

    @Override
    public String get_user_quoteFillRatio() {
        return null;
    }
}

package binance.rest;

import binance.data.ErrorAPI;
import binance.data.MarkPrice;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static utils.Auth.encode_hmac;

public class RestImp implements Rest {

    private static final Logger logger = LogManager.getLogger(binance.rest.RestImp.class.getName());

    private final Gson g;
    private final String url;
    private final Client client;
    private final String apiKey;
    private final String apiSecret;

    public RestImp(String url, String apiKey, String apiSecret) {
        this.g = new Gson();
        this.url = url;
        this.client = client_configuration();
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    /**
     * @param verb     - 'GET', 'POST', 'DELETE', 'PUT'
     * @param endpoint - endpoint on server
     * @param data     - data sent either in url ('GET') or in the body
     * @param signed   - true if request need to be signed, false otherwise
     * @param k - type of object to return
     * @return error message if request could not be retried, or retried request response (both as String)
     */
    private Object api_call(String verb, String endpoint, JsonObject data, boolean signed, Class<?> k) {
        WebTarget target = client.target(url).path(endpoint);
        if (verb.equalsIgnoreCase("GET")) {
            for (String name : data.keySet()) {
                target = target
                        .queryParam(name, URLEncoder.encode(data.get(name).getAsString(), StandardCharsets.UTF_8));
            }
        }

        if(signed) {
            if(verb.equalsIgnoreCase("GET")) {
                target = target
                        .queryParam("timestamp", System.currentTimeMillis());
                target = target
                        .queryParam("signature", URLEncoder.encode(encode_hmac(this.apiSecret, target.getUri().getQuery()), StandardCharsets.UTF_8));
            }else {
                data.addProperty("timestamp", System.currentTimeMillis());
                data.addProperty("signature", encode_hmac(this.apiSecret, data.toString()));
            }
        }

        Invocation.Builder httpReq = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .header("content-type", "application/json; charset=utf-8")
                .header("connection", "keep-alive");

        logger.debug(String.format("Making API request: %s", target.getUri().toString()));
        if(!verb.equalsIgnoreCase("GET"))
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
                    if(status == Response.Status.OK.getStatusCode())
                        return g.fromJson(r.readEntity(String.class), k);
                    else
                        return api_error(status, verb, endpoint, data, r.readEntity(ErrorAPI.class), signed, k, r.getHeaders());
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

    private Object api_error(int status, String verb, String endpoint, JsonObject data, ErrorAPI errObj, boolean signed, Class<?> k, MultivaluedMap<String, Object> headers) {
        return "";
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

    @Override
    public MarkPrice get_mark_price(@NotNull String symbol) {
        try {
            JsonObject params = new JsonObject();
            params.addProperty("symbol", symbol);
            return (MarkPrice) api_call("GET", "/fapi/v1/premiumIndex", params, false, MarkPrice.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

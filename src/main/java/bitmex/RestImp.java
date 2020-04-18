package bitmex;

import bitmex.Exceptions.ApiConnectionError;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.logging.Logger;

import static org.apache.commons.codec.digest.HmacAlgorithms.HMAC_SHA_256;

public class RestImp implements Rest {

    private final static Logger LOGGER = Logger.getLogger(Rest.class.getName());
    private final Gson g = new Gson();
    private Client client;
    private String apiKey, apiSecret;
    private boolean shouldAuth;

    /**
     * Implementation to connect to the Bitmex Rest API, see more at https://www.bitmex.com/api/explorer/
     * @param apiKey - apiKey of client, "" otherwise
     * @param apiSecret - apiSecret of client, "" otherwise
     */
    public RestImp(String apiKey, String apiSecret) {
        client = clientConfiguration();
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        if (!apiKey.equals("") && !apiSecret.equals(""))
            shouldAuth = true;
        else
            shouldAuth = false;
    }

    public JsonArray apiCall(String verb, String endpoint, JsonObject data) throws InvalidKeyException, NoSuchAlgorithmException, ApiConnectionError {
        WebTarget target = client.target(Rest.url).path(Rest.apiPath + endpoint);
        if (verb.equalsIgnoreCase("GET")) {
            for (String name : data.keySet()) {
                target = target
                        .queryParam(name, data.get(name).getAsString());
            }
        }

        Invocation.Builder httpReq = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .header("content-type", "application/json; charset=utf-8")
                .header("connection", "keep-alive");

        if (shouldAuth) {
            String expires = String.valueOf(Instant.now().getEpochSecond() + 3600);
            String signature = encodeHmacSignature(apiSecret, verb + target.getUri().getPath() + expires + data.toString());
            httpReq = httpReq
                    .header("api-expires", expires)
                    .header("api-key", apiKey)
                    .header("api-signature", signature);
        }

        boolean success = false;
        while (!success) {
            try {
                Response r = null;
                LOGGER.info(target.getUri().toString());
                if (verb.equalsIgnoreCase("GET"))
                    r = httpReq.get();
                else if(verb.equalsIgnoreCase("POST"))
                    r = httpReq.post(Entity.entity(data, MediaType.APPLICATION_JSON));
                else if(verb.equalsIgnoreCase("PUT"))
                    r = httpReq.put(Entity.entity(data, MediaType.APPLICATION_JSON));
                else if(verb.equalsIgnoreCase("DELETE"))
                    r = httpReq.build("DELETE", Entity.entity(data, MediaType.APPLICATION_JSON)).invoke();

                // get http response code
                int status = r.getStatus();
                LOGGER.info(String.valueOf(status));
                success = true;

                if (status == Response.Status.OK.getStatusCode() && r.hasEntity())
                    return JsonParser.parseString(r.readEntity(String.class)).getAsJsonArray();
                else
                    apiError(status);

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
        //connection error
        throw new ApiConnectionError();
    }

    /**
     * Build and configures a Jersey client
     *
     * @return the entry point to the API to execute client requests
     */
    private Client clientConfiguration() {
        ClientConfig config = new ClientConfig();
        //How much time until timeout on opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, Rest.CONNECTION_TIMEOUT);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, Rest.REPLY_TIMEOUT);
        //Property to allow to post body data in a 'DELETE' request, otherwise an exception is thrown
        config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
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
     * @throws InvalidKeyException      - invalid Keys (invalid encoding, wrong length, uninitialized)
     * @throws NoSuchAlgorithmException - cryptographic algorithm is requested but is not available
     */
    public static String encodeHmacSignature(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance(HMAC_SHA_256.getName());
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256.getName());
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private void apiError(int status) {

    }
}

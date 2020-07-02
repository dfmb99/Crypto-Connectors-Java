package itbit;

import bitmex.rest.Rest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;
import java.util.logging.Logger;

public class WsImp {
    private final static Logger LOGGER = Logger.getLogger(WsImp.class.getName());
    private final static String URL = "https://api.itbit.com/v1/";
    private final Gson g;
    private final String symbol;
    private float lastPrice;

    // connection constants
    private final static int CONNECTION_TIMEOUT = 5000;
    private final static int REPLY_TIMEOUT = 5000;
    private final static int RETRY_PERIOD = 3000;

    public WsImp(String symbol) {
        this.g = new Gson();
        this.symbol = symbol;
        this.lastPrice = -1f;
        new Thread(this::poll_server).start();
        this.waitForData();
    }

    private void poll_server() {
        while (true) {
            this.lastPrice = g.fromJson(Objects.requireNonNull(get_ticker_call()), JsonObject.class).get("lastPrice").getAsFloat();
        }
    }

    /**
     * Return ticker call
     *
     * @return String - response
     */
    private String get_ticker_call() {
        Client client = client_configuration();

        WebTarget target = client.target(URL).path(String.format("markets/%s/ticker", this.symbol));

        Invocation.Builder httpReq = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .header("content-type", "application/json; charset=utf-8")
                .header("connection", "keep-alive");

        boolean success = false;
        while (!success) {
            try {
                Response r = httpReq.get();

                int status = r.getStatus();
                success = true;
                LOGGER.fine("Received http response for ticker call.");

                if (status == Response.Status.OK.getStatusCode() && r.hasEntity())
                    return r.readEntity(String.class);
                else {
                    try {
                        Thread.sleep(RETRY_PERIOD);
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                    // retry request
                    return get_ticker_call();
                }

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
        LOGGER.severe("Connection error.");
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
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        //How much time to wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
        //Allow changing http headers, before a request
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        return ClientBuilder.newClient(config);
    }

    /**
     * Returns last price
     *
     * @return last price
     */
    public float get_last_price() {
        return this.lastPrice;
    }

    /**
     * waits for instrument ws data, blocking thread
     */
    private void waitForData() {
        while (this.lastPrice < 0.0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

}

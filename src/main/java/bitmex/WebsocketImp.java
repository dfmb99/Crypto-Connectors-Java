package bitmex;

import javax.websocket.*;
import java.net.URI;
import java.util.logging.Logger;

@ClientEndpoint
public class WebsocketImp {

    private final static Logger LOGGER = Logger.getLogger(Bitmex.class.getName());
    private Session userSession;

    /**
     * Bitmex WebSocket client implementation
     * @param url - ws endpoint to connect
     */
    public WebsocketImp(String url) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, URI.create(url));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param reason      the reason for connection close
     */
    @OnClose
    public void onClose(CloseReason reason) {
        LOGGER.warning(String.format("Websocket closed with code: %d \n Message: %s", reason.getCloseCode(), reason.getReasonPhrase()));
        this.userSession = null;
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        System.out.println(message);
    }

    /**
     * Callback hook for Error Events. This method will be invoked when a client receives a error.
     *
     * @param userSession
     * @param throwable
     */
    @OnError
    public void onError(Session userSession, Throwable throwable) {
        LOGGER.warning(throwable.getMessage());
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

}

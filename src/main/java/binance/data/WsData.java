package binance.data;

import com.google.gson.JsonObject;

public class WsData {
    private String stream;
    private JsonObject data;

    public String getStream() {
        return stream;
    }

    public JsonObject getData() {
        return data;
    }
}

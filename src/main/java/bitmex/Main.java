package bitmex;

import bitmex.Exceptions.ApiConnectionError;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, ApiConnectionError {
        RestImp rest = new RestImp("rRed-X2VHyRw3s4fBXg3U0eS", "MMXqbn1YoIomf2GZml9zdA_qzKDSIXkJaVqbFXBEOImv5ei2");
        JsonObject obj = JsonParser.parseString("{'symbol': 'XBTUSD'}").getAsJsonObject();
        String data = rest.apiCall("GET", "/instrument", obj);
        System.out.println(data.toString());
    }
}

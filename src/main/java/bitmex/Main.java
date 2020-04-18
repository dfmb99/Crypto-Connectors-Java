package bitmex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException{
        RestImp rest = new RestImp("", "");
        JsonObject obj = JsonParser.parseString("{'symbol': 'XBTUSD'}").getAsJsonObject();
        JsonArray data = rest.apiCall("GET", "/instrument", obj);
        System.out.println(data.size());
        obj = data.get(0).getAsJsonObject();
        for (String name : obj.keySet()) {
            System.out.println(name + ": " + obj.get(name).toString());
        }
    }
}

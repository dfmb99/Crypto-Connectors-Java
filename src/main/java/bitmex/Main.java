package bitmex;

import com.google.gson.JsonObject;

public class Main {
    public static void main(String[] args) {

        RestImp rest = new RestImp("rRed-X2VHyRw3s4fBXg3U0eS", "MMXqbn1YoIomf2GZml9zdA_qzKDSIXkJaVqbFXBEOImv5ei2");
        JsonObject obj = rest.get_user_margin();
        System.out.println(obj.toString());
    }
}

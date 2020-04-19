package bitmex;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {
    public static void main(String[] args) {

        RestImp rest = new RestImp("rRed-X2VHyRw3s4fBXg3U0eS", "MMXqbn1YoIomf2GZml9zdA_qzKDSIXkJaVqbFXBEOImv5ei2");
        JsonObject obj =
                JsonParser.parseString("{'orderID': ['22887744-c305-dc15-5444-dbddd553273b', " +
                        "'598297c9-5446-aedb-0d16-156c618589a0']}").getAsJsonObject();
        String data = rest.del_order(obj);
        System.out.println(data);
    }
}

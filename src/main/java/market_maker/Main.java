package market_maker;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.logging.Logger;

public class Main {

    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Gson g = new Gson();
        long start = System.currentTimeMillis();
        System.out.println("{\"ordStatus.isTerminated\":false}");
        System.out.println(JsonParser.parseString("{\"ordStatus.isTerminated\": false}").toString());
        System.out.println(System.currentTimeMillis() - start);

    }

}




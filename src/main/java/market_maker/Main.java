package market_maker;

import bitmex.data.Order;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.UuidUtil;

import java.util.Base64;
import java.util.UUID;

public class Main {
    static final Logger logger = LogManager.getLogger(Main.class.getName());


    public static void main(String[] args) {
        Order[] test = new Order[0];
        for(Order o: test)
            System.out.println(o.getOrderID());
        System.out.println(test.length);
    }

}




package market_maker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.UuidUtil;

import java.util.Base64;
import java.util.UUID;

public class Main {
    static final Logger logger = LogManager.getLogger(Main.class.getName());


    public static void main(String[] args) {
        System.out.println(("mmbitmex" + UuidUtil.getTimeBasedUuid().toString()).substring(0, 36));
        System.out.println(Base64.getEncoder().encodeToString((UUID.randomUUID().toString()).getBytes()).substring(0, 28));
    }

}




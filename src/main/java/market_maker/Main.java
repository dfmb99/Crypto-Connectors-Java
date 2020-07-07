package market_maker;

import bitmex.data.Instrument;

import java.util.logging.Logger;

public class Main {

    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
       Instrument i = new Instrument();

       System.out.println(i.getAskPrice());

    }

}




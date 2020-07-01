package market_maker;

public class Settings {
    // Main configuration
    public static final String SYMBOL = "XBTUSD";
    public static final boolean TESTNET = true;
    public static final boolean DRY_RUN = false;
    public static final String ORDER_ID_PREFIX = "mmbitmex";

    // Authentication MAINNET
    //public static final String API_KEY = "7oc_GCgs3A6T63f-AgyZ_cdo";
    //public static final String API_SECRET = "AuOWZ3wnmtiF7En9gv98f80G2fZ6zEojTPcnFv5mowGUy-ry";

    // Authentication TESTNET
    public static final String API_KEY = "_DnJPkOPL5DLemZnjhh1KQRO";
    public static final String API_SECRET = "u5-PUEPK6LMQoxF4LakHtmCD4nO_jbFfsPkeNFznTFyP4O9P";

    // Market making configuration
    public static final long ORDER_SIZE = 20L; // single order size
    public static final float SPREAD_MAINTAIN_RATIO = 1.3f;
    public static final float SPREAD_INDEX = 30f;
    public static final float SPREAD_INDEX_FACTOR = 0.9f;
    public static final int SPREAD_SNAPS = 720;
    public static final boolean POST_ONLY = true;
    public static final boolean MARK_PRICE_CALC = false; // calculate  mark price from spot exchanges
    public static final boolean MARK_PRICE_QUOTE_MID_PRICE = false; // use last order filled price or mark price as quote mid price for next orders

    // Timeouts

    // How long to wait in each cycle to check / post orders
    public static final int LOOP_INTERVAL = 500;
    // How often to perform sanity checks
    public static long SANITY_CHECK_INTERVAL = 60000L; // 1minute

    // Minimum spread in ticks
    public static final int MIN_SPREAD_TICKS = 10;

    // Position limits
    public static final boolean CHECK_POSITION_LIMITS = true;
    public static final long MIN_POSITION = -200L;
    public static final long MAX_POSITION = 200L;
}

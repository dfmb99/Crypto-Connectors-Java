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
    public static final int TRADE_BIN_SIZE = 240;
    public static final float QUOTE_SPREAD = 10f; // realized volatility minutes to base our spread index
    public static final float QUOTE_SPREAD_FACTOR = 0.9f;
    public static final int MIN_SPREAD_TICKS = 0; // Minimum spread in ticks
    public static final boolean POST_ONLY = false;

    // Position limits
    public static final boolean CHECK_POSITION_LIMITS = true;
    public static final long MIN_POSITION = -200L;
    public static final long MAX_POSITION = 200L;
}

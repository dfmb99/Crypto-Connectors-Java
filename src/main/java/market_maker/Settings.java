package market_maker;

public class Settings {
    // Main configuration
    public static final String SYMBOL = "XRPUSD";
    public static final boolean TESTNET = false;
    public static final boolean DRY_RUN = false;
    public static final String ORDER_ID_PREFIX = "mmbitmex";

    // Authentication MAINNET
    public static final String API_KEY = "7oc_GCgs3A6T63f-AgyZ_cdo";
    public static final String API_SECRET = "AuOWZ3wnmtiF7En9gv98f80G2fZ6zEojTPcnFv5mowGUy-ry";


    // Authentication TESTNET
    //public static final String API_KEY = "_DnJPkOPL5DLemZnjhh1KQRO";
    //public static final String API_SECRET = "u5-PUEPK6LMQoxF4LakHtmCD4nO_jbFfsPkeNFznTFyP4O9P";

    // Market making configuration
    public static final long ORDER_SIZE = 80L;
    public static final float SPREAD_MAINTAIN_RATIO = 1.3f;
    public static final float SPREAD_INDEX = 40f;
    public static final int SPREAD_SNAPS = (int) (SPREAD_INDEX * 2.5);
    public static final boolean POST_ONLY = false;
    public static final boolean MARK_PRICE_CALC = false;

    // Timeouts

    // How long to wait in each cycle to check / post orders
    public static final int LOOP_INTERVAL = 500;
    // How often to perform sanity checks
    public static long SANITY_CHECK_INTERVAL = 60000L; // 1minute
    // How often to log markPrice warnings (Eg: if calculated markPrice differs from websocket markPrice for more than 0.5%)
    public static long MARK_PRICE_LOG_INTERVAL = 60000L * 5L; // 5 minute
    // prevents getting whole account liquidated by not allowing orders that would increase position size when we are using over x of margin
    public static float MAX_MARGIN_USED = 0.4f;

    // Minimum spread in ticks
    public static final int MIN_SPREAD_TICKS = 10;

    // Position limits
    public static final boolean CHECK_POSITION_LIMITS = true;
    public static final long MIN_POSITION = -800L;
    public static final long MAX_POSITION = 800L;
}

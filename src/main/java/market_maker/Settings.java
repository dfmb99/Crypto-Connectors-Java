package market_maker;

public class Settings {
    public static final boolean TESTNET = true;
    public static final boolean DRY_RUN = false;
    public static final String ORDER_ID_PREFIX = "mmbitmex";

    // Authentication MAINNET
    //public static final String API_KEY = "7oc_GCgs3A6T63f-AgyZ_cdo";
    //public static final String API_SECRET = "AuOWZ3wnmtiF7En9gv98f80G2fZ6zEojTPcnFv5mowGUy-ry";

    // Authentication TESTNET
    public static final String API_KEY = "_DnJPkOPL5DLemZnjhh1KQRO";
    public static final String API_SECRET = "u5-PUEPK6LMQoxF4LakHtmCD4nO_jbFfsPkeNFznTFyP4O9P";

    public static final String[] SYMBOL = {"XBTUSD", "ETHUSD"};

    // Automatic order size calculation
    public static final boolean[] FLEXIBLE_ORDER_SIZE = {true, true};
    public static final float[] POS_MAX_MARGIN = {10f, 10f}; // maximum percentage of account to be used as position margin
    public static final float[] POSITION_FACTOR = {10f, 10f}; // orderSize * positionFactor = maximumPosition / -minimumPosition

    // Manual order size calculation
    public static final long[] ORDER_SIZE = {20L, 20L}; // single order size
    public static final long[] MIN_POSITION = {-200L};
    public static final long[] MAX_POSITION = {200L};

    // Market making settings
    public static final float[] SPREAD_MAINTAIN_RATIO = {1.3f, 1.3f};
    public static final int[] TRADE_BIN_SIZE = {480, 480};
    public static final float[] QUOTE_SPREAD = {40f, 40f}; // realized volatility minutes to base our spread index
    public static final float[] QUOTE_SPREAD_FACTOR = {0.8f, 0.8f};
    public static final float[] QUOTE_MIN_SPREAD = {0.002f, 0.002f}; // Minimum spread to quote (eg: 0.002 = 0.2% minium spread)
    public static final boolean[] POST_ONLY = {true, true};
    public static final boolean[] CHECK_POSITION_LIMITS = {true, true};

}

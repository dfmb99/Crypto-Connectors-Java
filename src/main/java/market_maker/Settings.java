package market_maker;

public class Settings {
    // Main configuration
    public static final String SYMBOL = "XBTUSD";
    public static final boolean TESTNET = true;
    public static final boolean DRY_RUN = false;
    public static final String ORDER_ID_PREFIX = "mmbitmex";

    // Authentication
    public static final String API_KEY = "PIrle0KczKUZdAt4rtKZSUdI";
    public static final String API_SECRET = "VewvFl3tbdirJ_yKaJs9OZXr0srqLo3L08PHFt6eHk1h3Tk3";

    // Market making configuration
    public static final long ORDER_SIZE = 10;
    public static final float SPREAD_MAINTAIN_RATIO = 1.3f;
    public static final float SPREAD_INDEX = 40f;
    public static final int SPREAD_SNAPS = (int) (SPREAD_INDEX * 2);
    public static final boolean POST_ONLY = false;

    // Timeouts

    // How long to wait in each cycle to check / post orders
    public static final int LOOP_INTERVAL = 500;
    // How long to wait after placing / amending orders
    public static final int REST_INTERVAL = 3000;
    // How often to perform sanity checks
    public static long SANITY_CHECK_INTERVAL = 60000;

    // Minimum spread in ticks
    public static final int MIN_SPREAD_TICKS = 10;

    // Position limits
    public static final boolean CHECK_POSITION_LIMITS = true;
    public static final long MIN_POSITION = -200;
    public static final long MAX_POSITION = 200;
}

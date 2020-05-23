package market_maker;

public class Settings {
    // Main configuration
    public static final String SYMBOL = "XBTUSD";
    public static final boolean TESTNET = true;
    public static final boolean DRY_RUN = false;
    public static final String ORDER_ID_PREFIX = "mmbitmex";

    // Authentication
    public static final String API_KEY = "9FHB_Fh_PU9NWjh4tVcM900f";
    public static final String API_SECRET = "4zfsknP2_mPRoCdmiFmDHvdtjyfi-8b5prEL0MWU04r0tLhb";

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
    public static final int REST_INTERVAL = 2000;

    // Minimum spread in ticks
    public static final int MIN_SPREAD_TICKS = 10;

    // Position limits
    public static final boolean CHECK_POSITION_LIMITS = true;
    public static final long MIN_POSITION = -200;
    public static final long MAX_POSITION = 200;
}

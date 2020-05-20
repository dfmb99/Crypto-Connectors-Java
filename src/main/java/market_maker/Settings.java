package market_maker;

public class Settings {
    public static final boolean TESTNET = true;
    public static final String API_KEY = "9FHB_Fh_PU9NWjh4tVcM900f";
    public static final String API_SECRET = "4zfsknP2_mPRoCdmiFmDHvdtjyfi-8b5prEL0MWU04r0tLhb";
    public static final String SYMBOL = "XBTUSD";
    public static final String ORDER_ID_PREFIX = "mmbitmex";
    public static final boolean DRY_RUN = false;
    public static final long ORDER_SIZE = 10;
    public static final float SPREAD_MAINTAIN_RATIO = 1.3f;
    public static final int VOLUME_INDEX_SNAPS = 40;
    public static final boolean POST_ONLY = false;

    // how long to wait in each cycle to check / post orders
    public static final int LOOP_INTERVAL = 500;
}

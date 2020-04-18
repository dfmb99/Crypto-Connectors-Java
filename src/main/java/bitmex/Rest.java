package bitmex;

public interface Rest {
    final static String url = "https://www.bitmex.com";
    final static String apiPath = "/api/v1";
    final static int CONNECTION_TIMEOUT = 3000;
    final static int REPLY_TIMEOUT = 1000;
    final static int RETRY_PERIOD = 1000;
}

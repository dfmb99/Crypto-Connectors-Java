package utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TimeStamp {

    public static long getTimestamp(String timestamp) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        try {
            return format.parse(timestamp).getTime();
        } catch (ParseException e) {
            // Do nothing
        }
        return -1L;
    }
}

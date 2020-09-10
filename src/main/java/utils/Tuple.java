package utils;

import org.jetbrains.annotations.NotNull;

public class Tuple<Y> implements Comparable<Tuple<Y>> {
    public final long timestamp;
    public final Y y;

    public Tuple(long timestamp, Y y) {
        this.timestamp = timestamp;
        this.y = y;
    }

    @Override
    public int compareTo(@NotNull Tuple<Y> o) {
        if( this.timestamp > o.timestamp )
            return 1;
        else if( this.timestamp < o.timestamp)
            return -1;
        else
            return 0;
    }
}

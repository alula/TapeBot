package space.alula.tapebot;

import java.util.concurrent.TimeUnit;

public class StringUtil {
    private StringUtil() {
        //
    }

    public static String codeEscape(String in) {
        return (in.contains("`")) ? in.replace("`", "`") : in;
    }

    public static String prettyPeriod(long time) {
        long secs = TimeUnit.MILLISECONDS.toSeconds(time) % 60;
        long mins = TimeUnit.MILLISECONDS.toMinutes(time) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(time);
        return hours != 0 ? String.format("%02d:%02d:%02d", hours, mins, secs)
                : String.format("%02d:%02d", mins, secs);
    }
}

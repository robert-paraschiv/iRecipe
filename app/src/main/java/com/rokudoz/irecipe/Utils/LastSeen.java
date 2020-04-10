package com.rokudoz.irecipe.Utils;

import android.text.format.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

public class LastSeen {
    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public LastSeen() {
    }

    public String getLastSeen(long time) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis() + 30000;
        if (time > now || time <= 0) {
            return "last seen just now";
        }
        Date timeStampDate = new Date(time);
        DateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        DateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());


        // TODO: localize
        final long diff = now - time;
        if (diff < 24 * HOUR_MILLIS && dayFormat.format(now).equals(dayFormat.format(timeStampDate))) {
            return "last seen today at " + hourFormat.format(timeStampDate);
        } else if (diff < 48 * HOUR_MILLIS) {
            return "last seen yesterday at " + hourFormat.format(timeStampDate);
        } else if (diff < 7 * DAY_MILLIS) {
            return "last seen " + dayFormat.format(timeStampDate) + " at " + hourFormat.format(timeStampDate);
        } else
            return "last seen " + diff / DAY_MILLIS + " days ago";
    }
}

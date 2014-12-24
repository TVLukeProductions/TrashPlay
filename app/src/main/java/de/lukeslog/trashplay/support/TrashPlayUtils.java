package de.lukeslog.trashplay.support;

import org.joda.time.DateTime;

public class TrashPlayUtils {

    public static String getStringFromIntInMilliSeconds(int p) {
        String result="";
        int m = 0;
        p = p / 1000;
        if (p > 59) {
            m = p / 60;
            if(m>0) {
                p = p - (60 * m);
            }
        }
        if (m > 9) {
            result = result + m;
        } else {
            result = result + "0" + m;
        }
        result = result + ":";
        if (p > 9) {
            result = result + p;
        } else {
            result = result + "0" + p;
        }
        return result;
    }


    public static boolean isChristmasTime() {
        DateTime now = new DateTime();
        return (now.getMonthOfYear()==12 && now.getDayOfMonth()>15 && now.getDayOfMonth()<28);
    }
}

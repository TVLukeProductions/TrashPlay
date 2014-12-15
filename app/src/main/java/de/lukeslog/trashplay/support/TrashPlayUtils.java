package de.lukeslog.trashplay.support;

public class TrashPlayUtils {

    public static String getStringFromIntInSeconds(int p) {
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
}

package de.lukeslog.trashplay.badges;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;

public class BadgeCollectionManager extends Service {

    public static final String BADGE_TYPE_ACTIVATION = "activation";
    public static final String BADGE_TYPE_NUMBER_OF_PLAYS = "numberofplays";
    public static final String BADGE_TYPE_PLAY_TIME = "playtime";
    public static final String BADGE_TYPE_RANDOM = "random";
    public static final String BADGE_TYPE_REPEAT = "repeat";
    public static final String BADGE_TYPE_LENGTH_OF_INSTALLATION = "installtime";
    public static final String BADGE_TYPE_HIGH_USAGE = "usageprecentage";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public ArrayList<Badge> getAllBadges() {
        ArrayList<Badge> allBadges = new ArrayList<Badge>();

        return allBadges;
    }


}

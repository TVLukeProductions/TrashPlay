package de.lukeslog.trashplay.lastfm;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.SettingsConstants;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class PersonalLastFM {

    public static final String TAG = TrashPlayConstants.TAG;

    public static void scrobble(final String artist, final String song, final SharedPreferences settings) {
        Logger.d(TAG, "scrobble personal");
        new Thread(new Runnable() {
            public void run() {
                Session session = getSession();
                if (session != null) {
                    int now = (int) (System.currentTimeMillis() / 1000);
                    ScrobbleResult result = Track.updateNowPlaying(artist, song, session);
                    result = Track.scrobble(artist, song, now, session);
                }
            }
        }).start();
    }

    protected static Session getSession() {
        Logger.d(TAG, "getSession");
        if (TrashPlayService.serviceRunning()) {
            SharedPreferences defsettings = PreferenceManager.getDefaultSharedPreferences(TrashPlayService.getContext());
            String lastfmusername = defsettings.getString(SettingsConstants.LASTFM_USER, "");
            String lastfmpassword = defsettings.getString(SettingsConstants.LASTFM_PSW, "");
            if (!lastfmusername.equals("") && !lastfmpassword.equals("")) {
                Logger.d(TAG, "it should scrobble");
                try {
                    return LastFM.getSession(lastfmusername, lastfmpassword);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}

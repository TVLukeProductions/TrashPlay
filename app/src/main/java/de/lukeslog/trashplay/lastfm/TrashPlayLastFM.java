package de.lukeslog.trashplay.lastfm;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.SettingsConstants;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class TrashPlayLastFM extends LastFM
{
    public static void scrobble(final String artist, final String song)
    {
        boolean trashmode = false;
        if(TrashPlayService.serviceRunning()) {
            SharedPreferences defSetting = PreferenceManager.getDefaultSharedPreferences(TrashPlayService.getContext());
            trashmode = defSetting.getBoolean(SettingsConstants.APP_SETTINGS_TRASHMODE, true);
            if(trashmode) {
                Logger.d(TAG, "scrobble to TrashPlay...");
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
        }
    }

    protected static Session getSession() {
        return LastFM.getSession(LastFMConstants.user, LastFMConstants.password);
    }
}

package de.lukeslog.trashplay.lastfm;

import android.content.SharedPreferences;

import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;

/**
 * Created by lukas on 02.12.14.
 */
public class PersonalLastFM {

    public static void scrobble(final String artist, final String song, final SharedPreferences settings)
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                Session session=getSession(settings);
                if(session!=null)
                {
                    int now = (int) (System.currentTimeMillis() / 1000);
                    ScrobbleResult result = Track.updateNowPlaying(artist, song, session);
                    result = Track.scrobble(artist, song, now, session);
                }
            }
        }).start();
    }

    protected static Session getSession(SharedPreferences settings) {
        //TODO: get Username and Password
        return LastFM.getSession(LastFMConstants.user, LastFMConstants.password);
    }
}

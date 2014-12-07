package de.lukeslog.trashplay.lastfm;

import android.util.Log;


import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class TrashPlayLastFM extends LastFM
{
    public static void scrobble(final String artist, final String song)
    {
        Log.d(TAG, "scrobble to TrashPlay...");
        new Thread(new Runnable()
        {
            public void run()
            {
                Session session=getSession();
                if(session!=null)
                {
                    int now = (int) (System.currentTimeMillis() / 1000);
                    ScrobbleResult result = Track.updateNowPlaying(artist, song, session);
                    result = Track.scrobble(artist, song, now, session);
                }
            }
        }).start();
    }

    protected static Session getSession() {
        return LastFM.getSession(LastFMConstants.user, LastFMConstants.password);
    }
}

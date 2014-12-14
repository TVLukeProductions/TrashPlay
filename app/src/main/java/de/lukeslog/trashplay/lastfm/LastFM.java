package de.lukeslog.trashplay.lastfm;

import android.util.Log;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.support.Logger;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;

public class LastFM {

    public static final String TAG = TrashPlayConstants.TAG;

    protected static Session getSession(String user, String password)
    {
        try
        {
            Caller.getInstance().setCache(null);
            return Authenticator.getMobileSession(user, password, LastFMConstants.key, LastFMConstants.secret);
        }
        catch(Exception e)
        {
            Log.e(TAG, "" + e.getMessage());
        }
        return null;
    }

    public static void getTrackInfo(final String artist, final String song)
    {
        try
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        Session session=getSession(LastFMConstants.user, LastFMConstants.password);
                        if(session!=null)
                        {
                            Track trackinfo = Track.getInfo(artist, song, LastFMConstants.key);
                            String imageurl = trackinfo.getImageURL(null);
                            Logger.d(TAG, "imageurl->" + imageurl);
                            Logger.d(TAG, "duration: "+trackinfo.getDuration());
                        }
                    }
                    catch(Exception e)
                    {
                        Log.e(TAG, ""+e.getMessage());
                    }
                }
            }).start();
        }
        catch(Exception e)
        {
            Log.e(TAG, ""+e.getMessage());
        }
    }
}

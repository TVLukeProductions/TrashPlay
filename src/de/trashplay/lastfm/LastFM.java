package de.trashplay.lastfm;

import android.content.SharedPreferences;
import android.util.Log;
import de.trashplay.main.TrashPlayConstants;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class LastFM 
{

	public static final String TAG = TrashPlayConstants.TAG;
	
	public static void scrobble(final String artist, final String song)
	{
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
	
	private static Session getSession()
	{
		try
		{
			Caller.getInstance().setCache(null);
			return Authenticator.getMobileSession(LastFMConstants.user,LastFMConstants.password, LastFMConstants.key, LastFMConstants.secret);
		}
		catch(Exception e)
		{
			Log.e(TAG, ""+e.getMessage());
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
	    	    		Session session=getSession();
	    				if(session!=null)
	    				{
			    	    	Track trackinfo = Track.getInfo(artist, song, LastFMConstants.key);
			    	    	String imageurl = trackinfo.getImageURL(null);
			    	    	Log.d(TAG, "imageurl->"+imageurl);
			    	    	Log.d(TAG, "duration: "+trackinfo.getDuration());
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

package de.trashplay.main;

import java.io.File;
import java.io.IOException;

import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

import de.trashplay.lastfm.LastFMConstants;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * Background Service for the TrashPlayer. It plays music until told otherwise. 
 * @author lukas
 *
 */
public class TrashPlayService extends Service implements OnPreparedListener
{

	public static boolean playing=false;
	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
	
    private MediaPlayer mp = new MediaPlayer();
    private int randomsongnumber;
	
    private Updater updater;
    public static DropboxAPI<AndroidAuthSession> mDBApi;
	public static boolean wifi=false;
    final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;

    String oldartist="";
    String oldtitle="";
    
	public class LocalBinder extends Binder 
    {
    	TrashPlayService getService() 
    	{
            // Return this instance of LocalService so clients can call public methods
            return TrashPlayService.this;
        }
    }
    
    private final IBinder mBinder = new LocalBinder();
    
    
	@Override
	public IBinder onBind(Intent intent) 
	{
		return mBinder;
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
		//Start foreground is another way to make sure the app does not stop...
		int icon = R.drawable.recopen; 
		Log.d(TAG, "on create Service");
		 Notification note=new Notification(icon, "TrashPlayer", System.currentTimeMillis());
		 Intent i=new Intent(this, MainActivity.class);

		 i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
				 Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent pi=PendingIntent.getActivity(this, 0,
               i, 0);

		note.setLatestEventInfo(this, "TrashPlayer",
				"...running",
				pi);
		note.flags |= Notification.FLAG_ONGOING_EVENT
				| Notification.FLAG_NO_CLEAR;
		
		startForeground(5646, note);
		return START_STICKY;
    }

	@Override
	public void onCreate() 
	{
		super.onCreate();
		Log.d(TAG, "notification should have been shown");
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
 		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

 		if (mWifi.isConnected()) 
 		{
	    	wifi=true;
	    } 
	    else 
	    {
	      wifi=false;
	    }
		getDropboxAPI();

		updater= new Updater();
        updater.run();
	}
	
	@Override
    public void onDestroy() 
    {
		Log.i(TAG, "onDestroy!");
        super.onDestroy();
        NotificationManager notificationManager = (NotificationManager) 
        		  getSystemService(NOTIFICATION_SERVICE); 
        notificationManager.cancel(5646);
        updater.onPause();
        mp.release();
        stopForeground(true);
    }
	
	private void goplaymp3()
	{
		Log.d(TAG, "go play mp3");
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} 
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} 
		else 
		{
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		if( mExternalStorageAvailable)
		{
			Log.i(TAG, "state: "+Environment.getExternalStorageState());
			File filesystem = Environment.getExternalStorageDirectory();
			String path = filesystem.getAbsolutePath();
			File[] filelist = filesystem.listFiles();
			for(int i=0; i<filelist.length; i++)
			{
				//Log.i(TAG, filelist[i].getName());
				if(filelist[i].getName().equals("Music"))
				{
					//Log.i(TAG, "found the music folder");
					File[] filelist2 = filelist[i].listFiles();
					for(int j=0; j<filelist2.length; j++)
					{
						//Log.i(TAG, filelist2[j].getName());
						if(filelist2[j].getName().equals("TrashPlay"))
						{
							//Log.i(TAG, "found the trashplay folder");
							File[] filelist3 = filelist2[j].listFiles();
							randomsongnumber = (int) (Math.random() * (filelist3.length));
							Log.d(TAG, "found "+filelist3.length+" files. PLaying "+randomsongnumber);
							String musicpath = filelist3[randomsongnumber].getAbsolutePath();
							File f = new File(musicpath);
							String artist="";
							String song="";
							try 
							{
								final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
								boolean lastfmactive = settings.getBoolean("lastfmactive", false);
								if(lastfmactive)
								{
									if(!oldtitle.equals(""))
									{
										scrobble(oldartist, oldtitle);
									}
									oldtitle="";
									oldartist="";
									MP3File mp3 = new MP3File(f);
									ID3v1 id3 = mp3.getID3v1Tag();
									artist = id3.getArtist();
									oldartist=artist;
									Log.d(TAG, "----------->ARTIST:"+artist);
									song = id3.getSongTitle();
									Log.d(TAG, "----------->SONG:"+song);
									oldtitle=song;
									playNow(artist, song);
								}
							} 
							catch (IOException e1) 
							{
								e1.printStackTrace();
							} 
							catch (TagException e1) 
							{
								e1.printStackTrace();
							}
							catch(Exception ex)
							{
								Log.e(TAG, "There has been an exception while extracting ID3 Tag Information from the MP3");
								String fn = f.getName();
								if(fn.contains("-") && fn.endsWith("mp3"))
								{
									fn=fn.replace(".mp3", "");
									String[] spl = fn.split("-");
									if(spl.length==2)
									{
										oldartist=spl[0];
										oldtitle=spl[1];
										playNow(spl[0], spl[1]);
									}
								}
							}
							try 
							{
								mp = new MediaPlayer();
								mp.setDataSource(musicpath);
								mp.setLooping(false);
						        mp.setVolume(0.99f, 0.99f);
						        mp.setOnPreparedListener(this);
						        mp.prepareAsync();
							} 
							catch (IllegalArgumentException e) 
							{
								e.printStackTrace();
							} 
							catch (IllegalStateException e) 
							{
								e.printStackTrace();
							} 
							catch (IOException e) 
							{
								e.printStackTrace();
							}

						}
					}
				}
			}
		}
	}
	
	@Override
	public void onPrepared(MediaPlayer mpx) 
	{
		mpx.setOnCompletionListener(new OnCompletionListener(){

			@Override
			public void onCompletion(MediaPlayer mpx) 
			{
				goplaymp3();
			}
			
		});
		mpx.start();
	}

	public void stop() 
	{
		Log.d(TAG, "stop");
		try
		{
			mp.stop();
			mp.release();
		}
		catch(Exception e)
		{
			
		}
		playing=false;
		onDestroy();
	}

	public void start() 
	{
		Log.d(TAG, "start");
		playing=true;
		try
		{
			goplaymp3();
		}
		catch(Exception e)
		{
			Log.e(TAG, "the mp3 playing is the problem");
			Log.e(TAG, e.getMessage());
		}
	}

	private DropboxAPI <AndroidAuthSession> getDropboxAPI()
	{
		//AppKeyPair appKeys = new AppKeyPair(DropBoxConstants.appKey, DropBoxConstants.appSecret);
		//AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
		//mDBApi = new DropboxAPI<AndroidAuthSession>(session);	
	
		
	    AppKeyPair appKeys = new AppKeyPair(DropBoxConstants.appKey, DropBoxConstants.appSecret);
	    AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
	    mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String dbkey = settings.getString("DB_KEY", "");
		String dbsecret= settings.getString("DB_SECRET", "");
	    if (dbkey.equals("")) return null;
	    AccessTokenPair access = new AccessTokenPair(dbkey, dbsecret);
	    mDBApi.getSession().setAccessTokenPair(access);
	    return mDBApi;
	}
	
	private class  Updater implements Runnable
    {
		 private Handler handler = new Handler();
         public static final int delay= 300000;//5 min
         SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
         @Override
         public void run() 
         {
        	 Log.d(TAG, "----------");
        	 final AudioManager audio;
        	 audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        	 int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        	 Log.d(TAG, "volume= "+volume);
        	 if(volume<5)
        	 {
        		 fadein();
        	 }
        	 if(wifi)
        	 {
        		 DropBox.syncFiles(settings);
        	 }
             handler.removeCallbacks(this); // remove the old callback
             handler.postDelayed(this, delay); // register a new one
         }
         
         public void onPause()
	     {
	         handler.removeCallbacks(this); // stop the map from updating
	     }
            
         public void onResume()
         {
         handler.removeCallbacks(this); // remove the old callback
         handler.postDelayed(this, delay); // register a new one
         }
    }
	
	private void scrobble(final String artist, final String song)
	{
	    new Thread(new Runnable() 
    	{
    	    public void run() 
    	    {
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			    String lastfmusername = LastFMConstants.user;
			    String lastfmpassword = LastFMConstants.password;
				if(!lastfmusername.equals(""))
				{
					Session session=null;
					try
					{
						Caller.getInstance().setCache(null);
						session = Authenticator.getMobileSession(lastfmusername, lastfmpassword, LastFMConstants.key, LastFMConstants.secret);
					}
					catch(Exception e)
					{
						Log.e(TAG, e.getMessage());
					}
					if(session!=null)
					{
						int now = (int) (System.currentTimeMillis() / 1000);
						ScrobbleResult result = Track.updateNowPlaying(artist, song, session);
						result = Track.scrobble(artist, song, now, session);
					}
				}
    	    }
    	}).start();
	}
	
	private void playNow(final String artist, final String song)
	{
	    new Thread(new Runnable() 
    	{
    	    public void run() 
    	    {
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			    String lastfmusername = LastFMConstants.user;
			    String lastfmpassword = LastFMConstants.password;
				if(!lastfmusername.equals(""))
				{
					Session session=null;
					try
					{
						Caller.getInstance().setCache(null);
						session = Authenticator.getMobileSession(lastfmusername, lastfmpassword, LastFMConstants.key, LastFMConstants.secret);
					}
					catch(Exception e)
					{
						Log.e(TAG, e.getMessage());
					}
					if(session!=null)
					{
						int now = (int) (System.currentTimeMillis() / 1000);
						ScrobbleResult result = Track.updateNowPlaying(artist, song, session);
						result = Track.updateNowPlaying(artist, song, session);
					}
				}
    	    }
    	}).start();
	}
	
	private void fadein()
	{
		final AudioManager audio;
		audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		new Thread(new Runnable() 
    	{
    	    public void run() 
    	    {
    			for(int x=0; x<16; x++)
    			{
    				audio.setStreamVolume(AudioManager.STREAM_MUSIC, x, AudioManager.FLAG_VIBRATE);
    				try
    		    	{
    		    		  Thread.currentThread().sleep(1500);
    		    	}
    		    	catch(Exception ie)
    		    	{

    		    	}
    			}
    	    }
    	}).start();
	}
}

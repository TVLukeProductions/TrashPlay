package de.trashplay.main;

import java.io.File;
import java.io.IOException;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Environment;
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
	
    // In the class declaration section:
    public static DropboxAPI<AndroidAuthSession> mDBApi;
    final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;

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
		return START_STICKY;
    }

	@Override
	public void onCreate() 
	{
		super.onCreate();
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
		Log.d(TAG, "notification should have been shown");
		getDropboxAPI();
	}
	
	@Override
    public void onDestroy() 
    {
		Log.i(TAG, "onDestroy!");
        super.onDestroy();
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
}

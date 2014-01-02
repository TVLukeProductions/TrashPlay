package de.trashplay.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

import de.trashplay.dropbox.*;
import de.trashplay.lastfm.LastFM;
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
import android.content.SharedPreferences.Editor;
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
	
    private Updater updater;
    public static DropboxAPI<AndroidAuthSession> mDBApi;
	public static boolean wifi=false;
    final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;

    String oldartist="";
    String oldtitle="";
    public static String file=""; 
    
	 AudioManager audio;
    
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
		getDropboxAPI();

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
 		file="";

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
		updater= new Updater();
        updater.run();
        
        
		return START_STICKY;
    }

	@Override
	public void onCreate() 
	{
		super.onCreate();
		Log.d(TAG, "notification should have been shown");

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
	
	private void playmp3()
	{
		Log.d(TAG, "go play mp3");
		boolean mExternalStorageAvailable = false;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
		    // We can read and write the media
		    mExternalStorageAvailable = true;
		} 
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
		{
		    // We can only read the media
		    mExternalStorageAvailable = true;
		} 
		else 
		{
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = false;
		}
		if(mExternalStorageAvailable)
		{
			File filesystem = Environment.getExternalStorageDirectory();
			ArrayList<File> filelist = getTrashMusicFiles(filesystem);	
			if(filelist!=null)
			{
				String nextSongFilePath = selectNextSong(filelist);
				final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				boolean lastfmactive = settings.getBoolean("lastfmactive", false);
				if(lastfmactive)
				{
					if(!oldtitle.equals(""))
					{
						LastFM.scrobble(oldartist, oldtitle);
					}
					oldartist="";
					oldtitle="";
				}
				String[] metadata = getMetaData(new File(nextSongFilePath));
				if(!metadata[0].equals("") && !metadata[1].equals(""))
				{
					oldartist=metadata[0];
					oldtitle=metadata[1];
				}
				try 
				{
					file=nextSongFilePath;//static variable to show which file is playing. 
					mp = new MediaPlayer();
					mp.setDataSource(nextSongFilePath);
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
	
	private String selectNextSong(ArrayList<File> filelist) 
	{
		Log.d(TAG, "found "+filelist.size()+" files.");
		final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String nextSong0 = settings.getString("nextSong0", "");
		String musicpath = "";
		Editor edit = settings.edit();
		if(nextSong0.equals(""))
		{
			//fill nextSong0 to nextSong9 with 10 songs to play next
			Log.d(TAG, "nextSong0 was totally nothing so we initiallize");
			for(int i=0; i<10; i++)
			{
				int randomsongnumber = (int) (Math.random() * (filelist.size()));
				String olol = filelist.get(randomsongnumber).getAbsolutePath();
				edit.putString("nextSong"+i, olol);	
				Log.d(TAG, olol);
			}
		}
		else
		{
			//make nextsSong1 to nextSong2 and so on and then select a new nextSong9
			Log.d(TAG, "we have a list of like 10 songs, so we take the first one...");
			for(int i=0; i<10; i++)
			{
				Log.d(TAG, settings.getString("nextSong"+i, ""));
			}
			musicpath = settings.getString("nextSong0", "");
			if(file.equals(musicpath))
			{
				musicpath = settings.getString("nextSong1", "");
				for(int i=0; i<9; i++)
				{
					edit.putString("nextSong"+i, settings.getString("nextSong"+(i+1), ""));
				}
				int randomsongnumber = (int) (Math.random() * (filelist.size()));
				String olol = filelist.get(randomsongnumber).getAbsolutePath();
				edit.putString("nextSong9", olol);
			}
			Log.d(TAG, "its"+musicpath);
			
			
		}
		edit.commit();
		musicpath = settings.getString("nextSong0", "");
		return musicpath;
		
	}

	public static String[] getMetaData(File file) 
	{
		String[] metadata = new String[2];
		metadata[0]="";
		metadata[1]="";
		try 
		{
			MP3File mp3 = new MP3File(file);
			ID3v1 id3 = mp3.getID3v1Tag();
			metadata[0] = id3.getArtist();
			Log.d(TAG, "----------->ARTIST:"+metadata[0]);
			metadata[1] = id3.getSongTitle();
			Log.d(TAG, "----------->SONG:"+metadata[1]);
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
			String fn = file.getName();
			fn.replace("–", "-"); //wired symbols that look alike
			if(fn.contains("-") && fn.endsWith("mp3"))
			{
				fn=fn.replace(".mp3", "");
				String[] spl = fn.split("-");
				if(spl.length==2)
				{
					metadata[0]=spl[0];
					metadata[0]=metadata[0].trim();
					metadata[0]=spl[1];
					metadata[0]=metadata[0].trim();
					Log.d(TAG, "----------->ARTIST():"+spl[0]);
					Log.d(TAG, "----------->SONG():"+spl[1]);
				}
			}
		}
		return metadata;
	}
	
	/**
	 * This method fetches the files from the 
	 * @param filesystem
	 * @return
	 */
	private ArrayList<File> getTrashMusicFiles(File filesystem) 
	{
		File[] filelist = filesystem.listFiles();
		ArrayList<File> fl = new ArrayList<File>();
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
						for(int k=0; k<filelist3.length; k++)
						{
							fl.add(filelist3[k]);
						}
						return fl;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void onPrepared(MediaPlayer mpx) 
	{
		mpx.setOnCompletionListener(new OnCompletionListener(){

			@Override
			public void onCompletion(MediaPlayer mpx) 
			{
				playmp3();
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
		updater.onPause();
		onDestroy();
	}

	public void start() 
	{
		Log.d(TAG, "start");
		playing=true;
		try
		{
			playmp3();
		}
		catch(Exception e)
		{
			Log.e(TAG, "the mp3 playing is the problem");
			Log.e(TAG, e.getMessage());
		}
	}

	private DropboxAPI <AndroidAuthSession> getDropboxAPI()
	{
	    AppKeyPair appKeys = new AppKeyPair(DBConstants.appKey, DBConstants.appSecret);
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
	
	/*
	 * This private class is a runnable which is basically in charge of checking fome values and instrucing some updates
	 * It checks the volume (and turns it up if its low)
	 * It checks wether we are in a wifi and if so, syncs the Dropbox
	 */
	private class  Updater implements Runnable
    {
		 private Handler handler = new Handler();
         public static final int delay= 5000;//5 sec
         SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
         long counter=0;
         @Override
         public void run() 
         {
        	 //check for the volume
        	 int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        	 //Log.d(TAG, "volume= "+volume);
        	 if(volume<5)
        	 {
        		 fadein();
        	 }
        	 //every 60 cicles (5 minutes) check if wifi and if so, dropbox sync
        	 if(counter%60==0) //every 5 minutes
        	 {
	        	 if(wifi)
	        	 {
	        		 DropBox.syncFiles(settings);
	        	 }
        	 }
        	 //counter
        	 counter++;
        	 //call the handler again
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
    		    		  Thread.currentThread().sleep(500);
    		    	}
    		    	catch(Exception ie)
    		    	{

    		    	}
    			}
    	    }
    	}).start();
	}
}

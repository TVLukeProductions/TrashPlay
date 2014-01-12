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
import de.trashplay.social.ContentManager;
import de.trashplay.social.TrashPlayerWebService;
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
public class TrashPlayServerService extends Service implements OnPreparedListener
{

	public static boolean playing=false;
	public static int loudness=0;
	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
	
    private static MediaPlayer mp = new MediaPlayer();
	
    private Updater updater;

    String oldartist="";
    String oldtitle="";
    public static String file=""; 
    public static ArrayList<File> songs = new ArrayList<File>();
    private int startId=0;
    
    static TrashPlayerWebService trashPlayerWebService=null;
    
	static AudioManager audio=null;
	 
	public static TrashPlayServerService ctx;
	 
	private final IBinder mBinder = new LocalBinder();
    
	public class LocalBinder extends Binder 
    {
    	TrashPlayServerService getService() 
    	{
            // Return this instance of LocalService so clients can call public methods
            return TrashPlayServerService.this;
        }
    }
   
    
	@Override
	public IBinder onBind(Intent intent) 
	{
		return mBinder;
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
		//Start foreground is another way to make sure the app does not stop...
		ctx=this;

 		file="";

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        changeInLoudness();
        
        //startService(new Intent(ctx, ContentManager.class));
		//ContentManager.startServer(this);
        
		updater= new Updater();
        updater.run();
        
        start();
        
		return START_STICKY;
    }

	@Override
	public void onCreate() 
	{
		super.onCreate();
		Log.d(TAG, "Server Service on Create");

	}
	
	@Override
    public void onDestroy() 
    {
        super.onDestroy();
		Log.i(TAG, "onDestroy!");
		stopForeground(true);
        NotificationManager notificationManager = (NotificationManager) 
        		  getSystemService(NOTIFICATION_SERVICE); 
        notificationManager.cancel(5646);
        updater.onPause();
        updater=null;
        Log.d(TAG, "bye!");
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
					if(trashPlayerWebService!=null)
					{
						trashPlayerWebService.setResourceStatus(file);
					}
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
	
	public static String playposition()
	{
		String result ="";
		if(mp!=null)
		{
			try 
			{
				int p = mp.getCurrentPosition();
				int m=0;
				p = p/1000;
				if(p>59)
				{
					m=p/60;
					p=p-(60*m);
				}
				if(m>9)
				{
					result=result+m;
				}
				else
				{
					result=result+"0"+m;
				}
				result=result+":";
				if(p>9)
				{
					result=result+p;
				}
				else
				{
					result=result+"0"+p;
				}
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return result;
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
				songs.add(i, new File(olol));
				Log.d(TAG, olol);
			}
		}
		else
		{
			//first check if the first songs in the list still exist... this is basically the only way to not listen to a file is to change the filename
			for(int i=0; i<2; i++)
			{
				String x = settings.getString("nextSong"+i, "");
				Log.d(TAG, x);
				boolean exist = false;
				for(int j=0; j<filelist.size(); j++)
				{
					if(filelist.get(j).getAbsolutePath().equals(x))
					{
						Log.d(TAG, "all good, the file "+i+" still exists");
						songs.add(i, new File(settings.getString("nextSong"+i, "")));
						exist=true;
					}
				}
				if(!exist)
				{
					Log.d(TAG, "Fuck, the file "+i+" has been renamed since... at some... well, anyway, choose a new one");
					int randomsongnumber = (int) (Math.random() * (filelist.size()));
					String olol = filelist.get(randomsongnumber).getAbsolutePath();
					edit.putString("nextSong"+i, olol);
					songs.add(i, new File(olol));
					edit.commit();
				}
			}
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
					songs.add(i, new File(settings.getString("nextSong"+(i+1), "")));
				}
				int randomsongnumber = (int) (Math.random() * (filelist.size()));
				String olol = filelist.get(randomsongnumber).getAbsolutePath();
				edit.putString("nextSong9", olol);
				songs.add(9, new File(olol));
			}
			if(songs.size()>10)
			{
				songs.clear();
			}
			if(songs.size()<10)
			{
				for(int i=0; i<10; i++)
				{
					songs.add(i, new File(settings.getString("nextSong"+(i), "")));
				}
				for(int i=10; i<songs.size(); i++)
				{
					songs.remove(i);
				}
			}
			Log.d(TAG, "its "+musicpath);
			
			
		}
		edit.commit();
		musicpath = settings.getString("nextSong0", "");
		return musicpath;
		
	}
	
	public static ArrayList<File> getSongList(SharedPreferences settings)
	{
		if(songs.size()==0)
		{
			for(int i=0; i<10; i++)
			{
				songs.add(i, new File(settings.getString("nextSong"+(i), "")));
			}
			return songs;
		}
		return null;
	}

	public static String[] getMetaData(File file) 
	{
		String[] metadata = new String[2];
		metadata[0]=file.getName();
		metadata[1]=" ";
		try 
		{
			//Log.d(TAG, "md1");
			MP3File mp3 = new MP3File(file);
			//Log.d(TAG, "md2");
			ID3v1 id3 = mp3.getID3v1Tag();
			//Log.d(TAG, "md3");
			//Log.d(TAG, "md3d");
			metadata[0] = id3.getArtist();
			//Log.d(TAG, "md4");
			//Log.d(TAG, "----------->ARTIST:"+metadata[0]);
			metadata[1] = id3.getSongTitle();
			//Log.d(TAG, "md5");
			//Log.d(TAG, "----------->SONG:"+metadata[1]);
			//Log.d(TAG, "md6");
		} 
		catch (IOException e1) 
		{
			e1.printStackTrace();
			metadata[0]=file.getName();
			metadata[1]=" ";
			//Log.d(TAG, "----------->ARTIST():"+metadata[0]);
			//Log.d(TAG, "----------->SONG():"+metadata[1]);
		} 
		catch (TagException e1) 
		{
			e1.printStackTrace();
			metadata[0]=file.getName();
			metadata[1]=" ";
			//Log.d(TAG, "----------->ARTIST():"+metadata[0]);
			//Log.d(TAG, "----------->SONG():"+metadata[1]);
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
					metadata[1]=spl[1];
					metadata[1]=metadata[1].trim();
					Log.d(TAG, "----------->ARTIST():"+spl[0]);
					Log.d(TAG, "----------->SONG():"+spl[1]);
				}
			}
			else
			{
				metadata[0]=file.getName();
				metadata[1]=" ";
				Log.d(TAG, "----------->ARTIST():"+metadata[0]);
				Log.d(TAG, "----------->SONG():"+metadata[1]);
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
		Log.d(TAG, "on Prepared");
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
		Log.d(TAG, "Service player stop");
		playing=false;
		updater.onPause();
		try
		{
			mp.stop();
			mp.release();
			mp=null;
		}
		catch(Exception e)
		{
			Log.e(TAG, "stop() had a problem when stoping the player....");
			playing=false;
		}
		stopSelf(startId);
	}

	public void start() 
	{
		Log.d(TAG, "Service Player start");
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
	
	public static TrashPlayServerService getService()
	{
		return ctx;
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
         boolean onpause=false;
         int loudness=0;
         @Override
         public void run() 
         {
        	 //check for the volume
        	 int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        	 //Log.d(TAG, "volume= "+volume);
        	 if(volume<1)
        	 {
        		 fadein();
        	 }
        	 if(volume!=loudness)
        	 {
        		 changeInLoudness();
        		 loudness=volume;
        	 }
        	 //TEST STUFF
        		 if(ContentManager.s!=null)
        		 {
        			 try
        			 {
	        			 Log.d(TAG, ContentManager.s.getName());
	        			 Log.d(TAG, ContentManager.s.getBaseUrl());
	        			 Log.d(TAG, " "+ContentManager.s.getPort());
        			 }
        			 catch(Exception e)
        			 {
        				 
        			 }
        		 }
        		 else
        		 {
        			 Log.e(TAG, "s is null");
        		 }
        	 //call the handler again
             handler.removeCallbacks(this); // remove the old callback
             handler.postDelayed(this, delay); // register a new one
         }
         
         public void onPause()
	     {
        	 Log.d(TAG, "updater on Pause in Service");
	         handler.removeCallbacks(this); // stop the map from updating
	     }
    }
	

	private void fadein()
	{
		final AudioManager audio;
		audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		final int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
		new Thread(new Runnable() 
    	{
    	    public void run() 
    	    {
    			for(int x=volume; x<3; x++)
    			{
    				Log.d(TAG, "v up");
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

	public static ArrayList<File> songlist() 
	{
		return songs;
	}

	public void registerForUpdates(TrashPlayerWebService trashPlayerWebService) 
	{
		this.trashPlayerWebService=trashPlayerWebService;
	}

	public static int louder(boolean b) 
	{
		Log.d(TAG, "loder/less loud");
		if(audio!=null)
		{
			int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
			Log.d(TAG, "volume-> "+volume);
			if(b)
			{
				Log.d(TAG, "louder");
				audio.setStreamVolume(AudioManager.STREAM_MUSIC, (volume+1), AudioManager.FLAG_VIBRATE);
				return (volume+1);
			}
			else
			{
				Log.d(TAG, "less loud");
				audio.setStreamVolume(AudioManager.STREAM_MUSIC, (volume-1), AudioManager.FLAG_VIBRATE);
				return (volume-1);
			}
		}
		return -5000;
	}
	
	private static void changeInLoudness()
	{
		Log.d(TAG, "change in loudness->");
		loudness = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
		if(trashPlayerWebService!=null)
		{
			trashPlayerWebService.setResourceStatus("");
		}
	}
}

package de.trashplay.main;

import java.io.File;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;

import de.trashplay.dropbox.DropBox;
import de.trashplay.main.TrashPlayService.LocalBinder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{

	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
    TrashPlayService mService;
    boolean mBound = false;
    long mStartTime;
    private UIUpdater updater;
    String display="";
    
    private static final int REQUEST_LINK_TO_DBX = 56;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//start the trashPlayService
		final ImageView playpause = (ImageView) findViewById(R.id.imageView1);
		ImageView sync = (ImageView) findViewById(R.id.imageView2);
		ImageView wifi = (ImageView) findViewById(R.id.imageView3);
		ImageView dropbox = (ImageView) findViewById(R.id.imageView4);
		final ImageView lastfm = (ImageView) findViewById(R.id.imageView5);
		sync.setVisibility(View.GONE);
		wifi.setVisibility(View.GONE);
		//playpause.setVisibility(View.GONE);
		getApplicationContext().startService(new Intent(this, TrashPlayService.class));
		Intent i = new Intent(this, TrashPlayService.class);		
		getApplicationContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE);
		
		playpause.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) 
			{
				Log.d(TAG, "click");
				if(TrashPlayService.playing)
				{
					if(mService!=null)
					{
						mService.stop();
						//playpause.setImageResource(R.drawable.play);
						MainActivity.this.finish();
					}
					else
					{
						Log.d(TAG, "mService==null");
					}
				}
				else
				{
					if(mService!=null)
					{
						mService.start();
						playpause.setImageResource(R.drawable.pause);
					}
					else
					{
						Log.d(TAG, "mService==null");
					}
				}
			}
			
		});
		playpause.setClickable(false);
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		dropbox.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) 
			{
				if(!settings.getString("DB_KEY", "").equals(""))
				{
					Log.d(TAG, "click dp");
					Editor edit = settings.edit();
					edit.putString("DB_KEY", "");
					edit.putString("DB_SECRET", "");
					edit.commit();
					toast("Disconnected from Dropbox");
				}
				else
				{
					Log.d(TAG, "click dp");
					TrashPlayService.mDBApi.getSession().startAuthentication(MainActivity.this);
					toast("Connecting to Dropbox");
				}
			}
			
		});
		final boolean lastfmactive = settings.getBoolean("lastfmactive", false);
		if(lastfmactive)
		{
			lastfm.setImageResource(R.drawable.lastfmlogoyes);
		}
		
		lastfm.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) 
			{
				final boolean lfma = settings.getBoolean("lastfmactive", false);
				Log.d(TAG, "click lfm");
				if(lfma)
				{
					lastfm.setImageResource(R.drawable.lastfmlogono);
					Editor edit = settings.edit();
					edit.putBoolean("lastfmactive", false);
					edit.commit();
					toast("Disconnected from Last.fm");
				}
				else
				{
					lastfm.setImageResource(R.drawable.lastfmlogoyes);
					Editor edit = settings.edit();
					edit.putBoolean("lastfmactive", true);
					edit.commit();
					toast("Connected to Last.fm");
				}
			}
			
		});
		updater= new UIUpdater();
        updater.run();
	}
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() 
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) 
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
        	Log.d(TAG, "bind stuff");
        	LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) 
        {
        	mService=null;
            mBound = false;
        }
    };

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onPause() 
	{
		updater.onPause();
		super.onPause();
	}
	
	@Override
	protected void onResume()
	{
		Log.d(TAG, "on Resume Main Activity");
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        DropBox.syncFiles(settings);
		updater.onResume();
		if(TrashPlayService.mDBApi!=null)
		{
		    if (TrashPlayService.mDBApi.getSession().authenticationSuccessful()) 
		    {
		        try 
		        {
		            // MANDATORY call to complete auth.
		            // Sets the access token on the session
		        	TrashPlayService.mDBApi.getSession().finishAuthentication();
		 
		            AccessTokenPair tokens = TrashPlayService.mDBApi.getSession().getAccessTokenPair();
		 
		            // Provide your own storeKeys to persist the access token pair
		            // A typical way to store tokens is using SharedPreferences
		            storeKeys(tokens.key, tokens.secret);
		        } 
		        catch (IllegalStateException e) 
		        {
		            Log.i("DbAuthLog", "Error authenticating", e);
		        }
		    }
		}
		super.onResume();
	}
	
	public void storeKeys(String key, String secret) 
	{
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		Editor edit = settings.edit();
		edit.putString("DB_KEY", key);
		edit.putString("DB_SECRET", secret);
		edit.commit();
		
	}

	private class  UIUpdater implements Runnable
    {
		 private Handler handler = new Handler();
         public static final int delay= 1000;
         SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
 		
         @Override
         public void run() 
         {
        	 File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/Music/TrashPlay");
        	 if(mService!=null)
        	 {
        		 if(TrashPlayService.mDBApi!=null && !settings.getString("DB_KEY", "").equals(""))
        		 {
        			ImageView playpause = (ImageView) findViewById(R.id.imageView1);
        			String[] x = folder.list();
        			if(!playpause.isClickable())
        			{
	        			if(x.length>10)
	        			{
	        				playpause.setImageResource(R.drawable.play);
	        				//playpause.setVisibility(View.VISIBLE);
	        				playpause.setClickable(true);
	        			}
        			}
        		 }
        	 }
        	 if(TrashPlayService.mDBApi!=null && !settings.getString("DB_KEY", "").equals(""))
    		 {
	        		 ImageView dropbox = (ImageView) findViewById(R.id.imageView4);
	        		 
	      		 dropbox.setImageResource(R.drawable.dropbox2);
	  		  }
        	 else
        	 {
        		 ImageView dropbox = (ImageView) findViewById(R.id.imageView4);
	        		
        		 dropbox.setImageResource(R.drawable.dropbox);
        	 }
        	 if(!TrashPlayService.file.equals(""))
        	 {
        		File f = new File(TrashPlayService.file);
        		if(TrashPlayService.file.equals(display))
        		{

        		}
        		else
        		{
        			display=TrashPlayService.file;
	        		String[] metadata = TrashPlayService.getMetaData(f);
	        		String newdisplay="";
	        		if(!metadata[0].equals("") && !metadata[1].equals(""))
	 				{
	        			 newdisplay=metadata[0]+" - "+metadata[1];
	 				}
	        		else
	        		{
	        			newdisplay=f.getName();
	        		}
	        		TextView textView1 = (TextView) findViewById(R.id.textView1);
	        		textView1.setText(newdisplay);
        		}
        	 }
        	 ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
 			 NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

 			if (mWifi.isConnected()) 
 			{
 				ImageView wifi = (ImageView) findViewById(R.id.imageView3);
 				wifi.setVisibility(View.VISIBLE);
 			    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
 			    //TrashPlayService.wifi=true;
 			
 			}
 			else
 			{
 				//TrashPlayService.wifi=false;
 			}
 			if(DropBox.syncinprogress)
 			{
 				ImageView sync = (ImageView) findViewById(R.id.imageView2);
 				sync.setVisibility(View.VISIBLE);
 			}
 			else
 			{
 				ImageView sync = (ImageView) findViewById(R.id.imageView2);
 				sync.setVisibility(View.GONE);
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
	
	   @Override
	    public void onActivityResult(int requestCode, int resultCode, Intent data) 
	    {
	    	Log.d("clock", "activity result");
	        if (requestCode == REQUEST_LINK_TO_DBX) 
	        {
	            if (resultCode == Activity.RESULT_OK) 
	            {
	            	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	                DropBox.syncFiles(settings);
	                toast("now syncing dropbox files. Please wait.");
	            } 
	            else 
	            {
	                Log.d("clock", "Link to Dropbox failed or was cancelled.");
	            }
	        } else 
	        {
	            super.onActivityResult(requestCode, resultCode, data);
	        }
	    }
	   
	   private void toast(String t)
	   {
		   Context context = getApplicationContext();
		   CharSequence text = t;
		   int duration = Toast.LENGTH_SHORT;

		   Toast toast = Toast.makeText(context, text, duration);
		   toast.show();
	   }

}

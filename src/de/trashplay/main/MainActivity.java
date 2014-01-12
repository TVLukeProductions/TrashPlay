package de.trashplay.main;

import java.io.File;
import java.util.ArrayList;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;

import de.trashplay.dropbox.DropBox;
import de.trashplay.lastfm.LastFM;
import de.trashplay.main.TrashPlayServerService.LocalBinder;
import de.trashplay.social.ContentManager;
import de.trashplay.social.NDSService;
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
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{

	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

	private Menu menu = null;
	
	private UIUpdater updater;
    String display="";
    Activity ctx;
    
    ArrayList<File> nexts = new ArrayList<File>();
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    
    int counter=0;
    
    private static final int REQUEST_LINK_TO_DBX = 56;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ctx=this;
		//start the trashPlayService
		final ImageView playpause = (ImageView) findViewById(R.id.imageView1);
		ImageView sync = (ImageView) findViewById(R.id.imageView2);
		ImageView wifi = (ImageView) findViewById(R.id.imageView3);
		sync.setVisibility(View.GONE);
		wifi.setVisibility(View.GONE);
		//playpause.setVisibility(View.GONE);
		startService(new Intent(ctx, TrashPlayService.class));
		
		playpause.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) 
			{
				Log.d(TAG, "click");
				if(TrashPlayServerService.playing)
				{
					if(TrashPlayServerService.ctx!=null)
					{
						TrashPlayServerService.ctx.stop();
					}
					ctx.stopService(new Intent(ctx, TrashPlayServerService.class));
					//ctx.stopService(new Intent(ctx, NDSService.class));
					MainActivity.this.finish();
				}
				else
				{
					startService(new Intent(ctx, TrashPlayServerService.class));
					//startService(new Intent(ctx, NDSService.class));
					playpause.setImageResource(R.drawable.pause);
					display="";
				}
			}
			
		});
		playpause.setClickable(false);
        
		ListView nextsongs = (ListView) findViewById(R.id.nextsongs);
		adapter=new ArrayAdapter<String>(this,
		                android.R.layout.simple_list_item_1,
		                listItems);
		ListView lv = (ListView) findViewById(R.id.nextsongs);
		lv.setAdapter(adapter);
		update();
		
		updater= new UIUpdater();
        updater.run();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    this.menu=menu;
	    inflater.inflate(R.menu.main, menu);
	    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    //set starting icon
	    for(int i=0; i<menu.size(); i++)
	    {
	    	MenuItem item = menu.getItem(i);
	    	if(item.getItemId()==R.id.dropbox)
	    	{
	    		
	    	}
	    	if(item.getItemId()==R.id.lastfm)
	    	{
	    		final boolean lastfmactive = settings.getBoolean("lastfmactive", false);
	    		if(lastfmactive)
	    		{
	    			Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogoyes);
					item.setIcon(myIcon);
	    		}
	    	}
	    }
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    // Handle presses on the action bar items
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    switch (item.getItemId()) 
	    {
	        case R.id.dropbox:
	        	if(!settings.getString("DB_KEY", "").equals(""))
				{
					Log.d(TAG, "click dp");
					Editor edit = settings.edit();
					edit.putString("DB_KEY", "");
					edit.putString("DB_SECRET", "");
					edit.commit();
					DropBox.syncinprogress=false;
					toast("Disconnected from Dropbox");
				}
				else
				{
					Log.d(TAG, "click dp");
					TrashPlayService.mDBApi.getSession().startAuthentication(MainActivity.this);
					toast("Connecting to Dropbox");
				}
	            return true;
	        case R.id.lastfm:
	        	final boolean lfma = settings.getBoolean("lastfmactive", false);
				Log.d(TAG, "click lfm");
				if(lfma)
				{
					Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogono);
					item.setIcon(myIcon);
					//lastfm.setImageResource(R.drawable.lastfmlogono);
					Editor edit = settings.edit();
					edit.putBoolean("lastfmactive", false);
					edit.commit();
					toast("Disconnected from Last.fm");
				}
				else
				{
					Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogoyes);
					item.setIcon(myIcon);
					//lastfm.setImageResource(R.drawable.lastfmlogoyes);
					Editor edit = settings.edit();
					edit.putBoolean("lastfmactive", true);
					edit.commit();
					toast("Connected to Last.fm");
				}
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	
	@Override
	protected void onPause() 
	{
		super.onPause();
		Log.d(TAG, "on Pause in Activity called");
		updater.onPause();
	}
	
	@Override
	protected void onStop()
	{
		 super.onStop();
		 Log.d(TAG, "on Stop in Activity called");
	}

	@Override
	protected void onDestroy()
	{	
		super.onDestroy();
		Log.d(TAG, "on Destroy in Activity called");
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d(TAG, "on Resume Main Activity");
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        //DropBox.syncFiles(settings);
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
		            Log.i("DbAuthLog", " Error authenticating", e);
		        }
		    }
		}
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
 		 String newdisplay="";
       
 		 @Override
         public void run() 
         {
        	 if(counter<0) //take care of posible overflow... unlikeley but possible
        	 {
        		 counter=0;
        	 }
        	 counter++;
        	 //Log.d(TAG, "run"+counter);
        	 File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/Music/TrashPlay");
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
       		 if(menu!=null)
       		 {
	        	 if(TrashPlayService.mDBApi!=null && !settings.getString("DB_KEY", "").equals(""))
	    		 {
	        		 for(int i=0; i<menu.size(); i++)
	        		 {
	        			MenuItem item = menu.getItem(i);
	        			if(item.getItemId()==R.id.dropbox)
	        			{
	        				Drawable myIcon = getResources().getDrawable(R.drawable.dropbox2);
	        				item.setIcon(myIcon);
	        			}
	        		}
	    		 }
	        	 else
	        	 {
	        		 for(int i=0; i<menu.size(); i++)
	        		 {
	        			MenuItem item = menu.getItem(i);
	        			if(item.getItemId()==R.id.dropbox)
	        			{
	        				Drawable myIcon = getResources().getDrawable(R.drawable.dropbox);
	        				item.setIcon(myIcon);
	        			}
	        		}
	        	 }
       		 }
        	 if(!TrashPlayServerService.file.equals(""))
        	 {
        		//Log.d(TAG, "run file not euals \"\"");
        		File f = new File(TrashPlayServerService.file);
        		if(TrashPlayServerService.file.equals(display))
        		{
        			//Log.d(TAG, "file equals display");
        		}
        		else
        		{
        			//Log.d(TAG, "x");
        			display=TrashPlayServerService.file;
	        		String[] metadata = TrashPlayServerService.getMetaData(f);
	        		if(!metadata[0].equals("") && !metadata[1].equals(""))
	 				{
	        			 newdisplay=metadata[0]+" - "+metadata[1];
	 				}
	        		else
	        		{
	        			newdisplay=f.getName();
	        		}
	        		nexts = TrashPlayServerService.songs;
	        		
	        		update();
        		}
        		//shif stuff

        		TextView position = (TextView) findViewById(R.id.posi);
        		String posix = TrashPlayServerService.playposition();
        		position.setText(posix);
        		
        		int shift = counter%newdisplay.length();
        		//Log.d(TAG, "shift: "+shift);
        		CharSequence d1 = newdisplay.subSequence(0, shift);
        		String s1Str = d1.toString();
        		CharSequence d2 = newdisplay.subSequence(shift, newdisplay.length());
        		String s2Str = d2.toString();
        		String ndx = s2Str+"+++"+s1Str;
        		TextView textView1 = (TextView) findViewById(R.id.songinfo);
        		textView1.setText(ndx);
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
        	 Log.d(TAG, "Activity update on Pause");
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

	    private void update()
	    {
	    	nexts.clear();
	    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    	nexts = TrashPlayServerService.getSongList(settings);
	    	listItems.clear();
	    	if(nexts!=null)
	    	{
		    	Log.d(TAG, "+-+-+-+-"+nexts.size());
	    		for(int i=0; i<(nexts.size()-1); i++)
	    		{
	    			File f=null;
	    			if(TrashPlayServerService.playing)
	    			{
	    				f = nexts.get(i+1);
	    			}
	    			else
	    			{
	    				f = nexts.get(i);
	    			}
	    			String newdisplay="";
	    			String[] metadata = TrashPlayServerService.getMetaData(f);
	        		if(!metadata[0].equals("") && !metadata[1].equals(""))
	 				{
	        			 newdisplay=(i+1)+": "+metadata[0]+" - "+metadata[1];
	        			 //LastFM.getTrackInfo(metadata[0], metadata[1]);
	 				}
	        		else
	        		{
	        			newdisplay=(i+1)+": "+f.getName();
	        		}
	        		listItems.add(i, newdisplay);
	    		}
	    		for(int i=8; i<listItems.size(); i++)
	    		{
	    			listItems.remove(i);
	    		}
	    	}
	        adapter.notifyDataSetChanged();
	    }
}

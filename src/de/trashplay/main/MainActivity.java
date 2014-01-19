package de.trashplay.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * The main activity of the app. It has to modes, called "clientmode" and "servermode", which use different 
 * layouts and menus and handle different usecases. They are both in this activity
 * to allow to be in the preset mode of the app right on startup.
 * 
 * 
 * 
 * @author lukas
 *
 */
public class MainActivity extends Activity
{

	//standard TAG and PREFS Constants
	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

	//the menu veriable is set on createOptionsmenu to be used in other methods to reset the icons
	private Menu menu = null;
	
	//the UI Updater Classes (ServerUI Updater or ClientUIUpdater) run to handle periodic updates to 
	//the UI and to the communcataion with the responsible service
	private ServerUIUpdater supdater;
	private ClientUIUpdater cupdater;
	
	//is set to true as soon as the playbutton is called. This is here because sometimes (for no apparent reason) the ServerService seems to start
	//if this is false it doesn't start playing mp3s
	public static boolean clicked=false;
    //the context (this)
    public static Activity ctx;
    
	//the current track file path to be displayed on top
    String display="";
    //next tracks
    ArrayList<File> nexts = new ArrayList<File>();
    //listItems from nexts
    ArrayList<String> listItems = new ArrayList<String>();
    //List adapter
    ArrayAdapter<String> adapter;
    
    //on first start the app asumes servermode
    boolean clientmode =false;
    
    //counter used for the scrolling text needs a variable to count where we are with scrolling
    int counter=0;
    
    //DB Stuff...
    private static final int REQUEST_LINK_TO_DBX = 56;
     
    @Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		ctx=this;
		Log.d(TAG, "1");
		final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		clientmode = settings.getBoolean("clientmode", false);
		Log.d(TAG, "2");
		if(clientmode)
		{
			setContentView(R.layout.activity_client);
			startActivity(new Intent(this, SearchForHubs.class));
			String ipstringx = settings.getString("ServerIP", "");
	
			startService(new Intent(ctx, TrashPlayService.class));
			startService(new Intent(ctx, TrashPlayClientService.class));			
			
			ImageView playbutton = (ImageView) findViewById(R.id.playbutton);
			playbutton.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) 
				{
					// TODO Auto-generated method stub
					if(TrashPlayClientService.ctx!=null)
					{
						TrashPlayClientService.ctx.startTrashPlayer();
					}
				}
				
			});
			
			ImageView offbutton = (ImageView) findViewById(R.id.offbutton);
			offbutton.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) 
				{
					if(TrashPlayClientService.ctx!=null)
					{
						TrashPlayClientService.ctx.stopTrashPlayer();
					}
				}
				
			});
			
			cupdater= new ClientUIUpdater();
	        cupdater.run();
		}
		else
		{
			startService(new Intent(ctx, ContentManager.class));
			ContentManager.startServer();
			setContentView(R.layout.activity_main);
			Log.d(TAG, "3");
			//start the trashPlayService
			final ImageView playpause = (ImageView) findViewById(R.id.imageView1);
			ImageView sync = (ImageView) findViewById(R.id.imageView2);
			ImageView wifi = (ImageView) findViewById(R.id.imageView3);
			sync.setVisibility(View.GONE);
			wifi.setVisibility(View.GONE);
			//playpause.setVisibility(View.GONE);
			startService(new Intent(ctx, TrashPlayService.class));
			Log.d(TAG, "4");
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
						ctx.stopService(new Intent(ctx, TrashPlayService.class));
						//ctx.stopService(new Intent(ctx, NDSService.class));
						MainActivity.this.finish();
					}
					else
					{
						ctx.startService(new Intent(ctx, TrashPlayServerService.class));
						clicked=true;
						//startService(new Intent(ctx, NDSService.class));
						playpause.setImageResource(R.drawable.pause);
						if(TrashPlayService.wifi)
						{
							 
						}
						display="";
					}
				}
				
			});
			Log.d(TAG, "5");
			playpause.setClickable(false);
			Log.d(TAG, "6");
			supdater= new ServerUIUpdater();
	        supdater.run();
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    this.menu=menu;
	    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	if(clientmode)
    	{
    		inflater.inflate(R.menu.servermain, menu);
			for(int i=0; i<menu.size(); i++)
			{
				MenuItem item = menu.getItem(i);
				if(item.getItemId()==R.id.lastfm)
		    	{
		    		final boolean lastfmactive = settings.getBoolean("lastfprivatemactive", false);
		    		if(lastfmactive)
		    		{
		    			Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogoyes_small);
						item.setIcon(myIcon);
		    		}
		    		else
		    		{
		    			Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogono_small);
						item.setIcon(myIcon);
		    		}
		    	}
			    if(item.getItemId()==R.id.clientmode)
			 	{
			    		item.setTitle("Server Mode");
				}
			}
		}
		else //Servermode
		{
			inflater.inflate(R.menu.servermain, menu);
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
			    			Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogoyes_small);
							item.setIcon(myIcon);
			    		}
			    		else
			    		{
			    			Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogono_small);
							item.setIcon(myIcon);
			    		}
			    	}
			    if(item.getItemId()==R.id.clientmode)
			 	{
			    		item.setTitle("Client Mode");
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
    	if(clientmode)
    	{
    		switch (item.getItemId()) 
		    {
		    	case R.id.lastfm:
		    		//here be either loging in to lastfm or other stuff...
		    		return true;
		    	case R.id.off:
		    		ctx.stopService(new Intent(ctx, TrashPlayService.class));
		    		MainActivity.this.finish();
		    		return true;
		    	case R.id.clientmode:
		        	Editor edit = settings.edit();
		        	edit.putBoolean("clientmode", false);
		        	item.setTitle("Server Mode");
					edit.commit();
		        	recreate();
		    		return true;
	        	default:
	        		return super.onOptionsItemSelected(item);
		    }
	    }
    	else
    	{
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
		    	case R.id.off:
		    		if(TrashPlayServerService.ctx!=null)
					{
						TrashPlayServerService.ctx.stop();
					}
		    		ctx.stopService(new Intent(ctx, TrashPlayService.class));
		    		MainActivity.this.finish();
		    		return true;
		        case R.id.clientmode:
		        	Log.d(TAG, "clientmode?");
		        	if(!TrashPlayServerService.playing)
		        	{
				        Editor edit = settings.edit();
				        edit.putBoolean("clientmode", true);
				        item.setTitle("Client Mode");
						edit.commit();
			        	recreate();
		        	}
		        	else
		        	{
		        		toast("Running Sever can not go into client mode.");
		        	}
		        	return true;
		        default:
		            return super.onOptionsItemSelected(item);
	    	}
    	}
	}
	
	
	@Override
	protected void onPause() 
	{
		Log.d(TAG, "on Pause in Activity called");
		super.onPause();
		if(clientmode)
		{
			Log.d(TAG, "we set pause on client mode...");
		}
		else
		{
			clicked=false;
			supdater.onPause();
		}

	}
	
	@Override
	protected void onStop()
	{
		clicked=false;
		super.onStop();
		Log.d(TAG, "on Stop in Activity called");
	}

	@Override
	protected void onDestroy()
	{	
		Log.d(TAG, "Main On Destroy");
		super.onDestroy();
		if(clientmode)
		{
			
		}
		else
		{
			if(!TrashPlayServerService.playing)
			{
				clicked=false;
				ContentManager.stopServer();
				ctx.stopService(new Intent(ctx, ContentManager.class));
			}
			else
			{
				toast("You can not switch into client mode while you are playing music");
			}
		}
		ctx=null;
		Log.d(TAG, "on Destroy in Activity called");
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d(TAG, "on Resume Main Activity");
    	final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        //DropBox.syncFiles(settings);
    	if(clientmode)
    	{
    		final Spinner ipspiner = (Spinner) findViewById(R.id.ipspiner); 
    		ArrayList<String> iplist = new ArrayList<String>();
    		String worklingips = settings.getString("workingHubIPs", "");
    		String[] wip = worklingips.split(",");
    		for(int i=0; i<wip.length; i++)
    		{
    			if(!wip[i].equals(""))
    			{
    				iplist.add(wip[i]);
    			}
    		}
 		    Log.d(TAG, "folderlistsize="+iplist.size());
 		    final List<String> spinnerArray = new ArrayList<String>();
 		    int sf = settings.getInt("selectedfolder", 0);
 		    
 		    for(int i=0; i<iplist.size(); i++)
 		    {
 			    spinnerArray.add(iplist.get(i));	

 		    }
 		    ipspiner.setOnItemSelectedListener(new OnItemSelectedListener() 
 			    {
 		    	@Override
 	            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
 		    	{
					Editor edit = settings.edit();
		        	edit.putString("ServerIP", spinnerArray.get(arg2));
					edit.commit();
 	            }
 	
 	            @Override
 	            public void onNothingSelected(AdapterView<?> arg0) 
 	            {
 	               
 		    		Log.d(TAG, "not selected");
 	            }
 		    });

 		    ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, spinnerArray);
 		    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 		    adapter.notifyDataSetChanged();
 		    ipspiner.setAdapter(adapter);
 		    ipspiner.setClickable(true);
 		    ipspiner.setSelected(true);
 		    adapter.notifyDataSetChanged();
 		    if(iplist.size()==1)
 		    {
 		    	ipspiner.setSelection(0);
 		    }
    	}
    	else
    	{
			supdater.onResume();
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
			Log.d(TAG, "populate lists");
			adapter=new ArrayAdapter<String>(this,
			                android.R.layout.simple_list_item_1,
			                listItems);
			ListView lv = (ListView) findViewById(R.id.nextsongs);
			lv.setAdapter(adapter);
			
			update();
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
	
	private class  ClientUIUpdater implements Runnable
    {

		@Override
		public void run() 
		{
			// TODO Auto-generated method stub
			
		}
		
    }

	private class  ServerUIUpdater implements Runnable
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
	        				Drawable myIcon = getResources().getDrawable(R.drawable.dropbox2_small);
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
	        				Drawable myIcon = getResources().getDrawable(R.drawable.dropbox_small);
	        				item.setIcon(myIcon);
	        			}
	        		}
	        	 }
       		 }
       		 if(TrashPlayServerService.playing)
       		 {
       		 final ImageView playpause = (ImageView) findViewById(R.id.imageView1);
	         if(!TrashPlayService.wifi && !ContentManager.runningServer())
	         {
				    MainActivity.this.runOnUiThread(new Runnable()
				    {
				        public void run()
				        {
				        	playpause.setImageResource(R.drawable.pause);
				        }
					});
	        	 }
	        	 if(TrashPlayService.wifi && ContentManager.runningServer())
	        	 {
	        		 new Thread(new Runnable() 
				     {
				    	    public void run() 
				    	    {
								try 
								{
									URL uri = new URL("http://chart.apis.google.com/chart?cht=qr&chs=300x300&chl=coap://"+ContentManager.getIPAddress(true)+":5683&choe=UTF-8");
									InputStream is = (InputStream) uri.getContent();
								    byte[] buffer = new byte[8192];
								    int bytesRead;
								    ByteArrayOutputStream output = new ByteArrayOutputStream();
								    while ((bytesRead = is.read(buffer)) != -1) 
								    {
								            output.write(buffer, 0, bytesRead);
								    }
								    byte[] bytes = output.toByteArray();
								    final Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
								    MainActivity.this.runOnUiThread(new Runnable()
								    {
								        public void run()
								        {
								        	playpause.setImageBitmap(bm);
								        }
								    });
								    
								} 
								catch (IOException e) 
								{
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
				    	    }
				    }).start();
	        	 }
       		 }
     		if(TrashPlayServerService.playing)
     		{
     			//Log.d(TAG, "fill position with time");
	        		TextView position = (TextView) findViewById(R.id.posi);
	        		String posix = TrashPlayServerService.playposition();
	        		String length = TrashPlayServerService.playlength();
	        		position.setText("["+posix+"|"+length+"]");
     		}
     		else
     		{
     			//Log.d(TAG, "fill position with number of tracks");
     			TextView position = (TextView) findViewById(R.id.posi);
     			int x = TrashPlayServerService.getSizeOfTrashPlaylist();
     			position.setText("["+x+"]");
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
        		//shif stuff in the field for title
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
	    	Log.d(TAG, "update");
	    	if(nexts!=null)
	    	{
	    		nexts.clear();
	    	}
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
	    	else
	    	{
	    		Log.d(TAG, "nexts is null");
	    	}
	    	try
	    	{
	    		adapter.notifyDataSetChanged();
	    	}
	    	catch(Exception e)
	    	{
	    		
	    	}
	    }
}

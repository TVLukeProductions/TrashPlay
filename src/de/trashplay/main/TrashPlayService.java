package de.trashplay.main;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

import de.trashplay.dropbox.DBConstants;
import de.trashplay.dropbox.DropBox;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class TrashPlayService extends Service 
{
    public static DropboxAPI<AndroidAuthSession> mDBApi;
    final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
	public static boolean wifi=false;
	
	public static final String TAG = TrashPlayConstants.TAG;
	public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
	
    private Updater updater;
	
	public static Context ctx;
    private int startId=0;
	
	@Override
	public IBinder onBind(Intent intent) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
		int icon = R.drawable.recopen; 
		Log.d(TAG, "on create Service");
		ctx=this;
		this.startId = startId;
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
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		getDropboxAPI();
		DropBox.syncFiles(settings);
		
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
	
	
	private DropboxAPI <AndroidAuthSession> getDropboxAPI()
	{
		Log.d(TAG, "getDPAPI");
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


	private class  Updater implements Runnable
    {
		 private Handler handler = new Handler();
         public static final int delay= 5000;//5 sec
         SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
         long counter=0;
         
         @Override
         public void run() 
         {        	 
        	 if(counter%60==0) //every 5 minutes
        	 {
	        	 if(wifi)
	        	 {
	        		 Log.d(TAG, "make it sync");
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
        	 Log.d(TAG, "updater on Pause in Service");
	         handler.removeCallbacks(this); // stop the map from updating
	     }
    }
}

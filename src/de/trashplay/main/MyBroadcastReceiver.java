package de.trashplay.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver 
{

        public static final String TAG = TrashPlayConstants.TAG;
        public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;
        
    @Override
    public void onReceive(Context context, Intent intent) 
    {
       Log.d(TAG, "onReceive");

        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (networkInfo != null) 
        {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) 
                {
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED) 
                        {
                                TrashPlayService.wifi=true;
                        }
                        else
                        {
                                TrashPlayService.wifi=false;
                        }
                }
        }

        }
}
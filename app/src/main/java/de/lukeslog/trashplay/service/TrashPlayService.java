package de.lukeslog.trashplay.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.cloudstorage.CloudSynchronizationService;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.player.MusicPlayer;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.ui.MainControl;

public class TrashPlayService extends Service {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    private Updater updater;

    public static boolean wifi = false;
    WifiBroadcastReceiver wbr = new WifiBroadcastReceiver();
    public static SharedPreferences settings;

    private static TrashPlayService ctx;

    public static SharedPreferences getDefaultSettings() {
        if(TrashPlayService.serviceRunning()) {
            return PreferenceManager.getDefaultSharedPreferences(TrashPlayService.getContext());
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        settings = getSharedPreferences(PREFS_NAME, 0);
        ctx=this;
        if (MainControl.activityRunning()) {

            //resetSyncFlagForRemote();
            registerWifiReceiver();

            Notification notification = createNotification("... running");
            startForeground(5646, notification);

            startUpdater();

            startMusicPlayerService();

            return START_STICKY;
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    private void resetSyncFlagForRemote() {
        CloudSynchronizationService.resetSyncFlag();
    }

    private void startMusicPlayerService() {
        startService(new Intent(ctx, MusicPlayer.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "notification should have been shown (main Service)");
    }

    public void stop()
    {
        Log.d(TAG, "Service player stop");
        if(MainControl.isPlayButtonClicked())
        {
            MusicPlayer.stopeverything();
            MainControl.ctx.finish();
        }
        //TODO Broadcast to the PlayerService that it is supposed to stop!
        updater.onPause();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopNotification();
        stopUpdater();
        ctx = null;
        Log.d(TAG, "bye!");
        super.onDestroy();
        //sometimes this stuff does not close even if everything has been shut down. this is a dirty hack to end all dirty hacks.
        //TODO: from time to time I should check if I can live without this
        android.os.Process.killProcess(android.os.Process.myPid());
        crash();
    }

    @SuppressWarnings("null")
    public void crash() {
        Object o = null;
        o.hashCode();
    }

    private void registerWifiReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.registerReceiver(wbr, intentFilter);
    }

    private void startUpdater() {
        updater = new Updater();
        updater.run();
    }

    private void stopUpdater() {
        if (updater != null) {
            updater.onPause();
            updater = null;
        }
    }

    private Notification createNotification(String notificationText) {
        int icon = R.drawable.recopen;
        Notification note = new Notification(icon, "TrashPlayer", System.currentTimeMillis());
        Intent i = new Intent(this, MainControl.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0,
                i, 0);

        note.setLatestEventInfo(this, "TrashPlayer",
                notificationText,
                pi);
        note.flags |= Notification.FLAG_ONGOING_EVENT
                | Notification.FLAG_NO_CLEAR;
        return note;
    }

    public static void updateNotificationText(String textToBeDisplayed) {
        if(serviceRunning()) {
            ctx.stopNotification();
            ctx.createNotification(textToBeDisplayed);
        }
    }

    public static boolean serviceRunning() {
        return ctx!=null;
    }

    private void stopNotification() {
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(5646);
    }

    public void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG);
    }

    private class Updater implements Runnable {

        private Handler handler = new Handler();
        public static final int delay = 5000;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long counter = 0;

        @Override
        public void run() {
       //     Log.d(TAG, "ServiceRunner: run");
            if(counter%60==0)
            {
                Log.d(TAG, "ServiceRunner: Time to try to synchronize and Stuff");
                try {
                    boolean lookForNewPlaylists=false;
                    if(counter%180==0){
                        lookForNewPlaylists=true;
                    }
                    MusicCollectionManager.getInstance().syncRemoteStorageWithDevice(lookForNewPlaylists);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            counter++;

            handler.removeCallbacks(this);
            handler.postDelayed(this, delay);
        }

        public void onPause() {
            Log.d(TAG, "updater on Pause in Service");
            handler.removeCallbacks(this);
        }

        public void onResume() {
            handler.removeCallbacks(this); // remove the old callback
            handler.postDelayed(this, delay); // register a new one
        }
    }

    public static TrashPlayService getContext() {
        return ctx;
    }

    class WifiBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "WIFI BROADCAST RECEIVER onReceive");

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                        if (serviceRunning()) {
                            TrashPlayService.wifi = true;
                            try {
                                MusicCollectionManager.getInstance().syncRemoteStorageWithDevice(false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (serviceRunning()) {
                            TrashPlayService.wifi = false;
                        }
                    }
                }
            }

        }
    }

    public void sendBroadcastToPause() {
        Logger.d(TAG, "send Broadcast to Pause");
        sendBroadcast(MusicPlayer.ACTION_PAUSE_SONG);
    }

    public void sendBroadcastToGoBack() {
        Logger.d(TAG, "send Broadcast to go back");
        sendBroadcast(MusicPlayer.ACTION_PREV_SONG);
    }

    public void sendBroadcastToStartNextSong() {
        Logger.d(TAG, "send Broadcast to Start the Next Song");
        sendBroadcast(MusicPlayer.ACTION_NEXT_SONG);
    }

    public void sendBroadcastToStartMusic() {
        Logger.d(TAG, "send Broadcast to Start the Music");
        sendBroadcast(MusicPlayer.ACTION_START_MUSIC);
    }

    public void sendBroadcastToStopMusic() {
        sendBroadcast(MusicPlayer.ACTION_STOP_MUSIC);
    }

    private void sendBroadcast(String action) {
        Logger.d(TAG, "SEND BROADCAST");
        Intent actionIntent = new Intent();
        actionIntent.setAction(action);
        TrashPlayService.getContext().sendBroadcast(actionIntent);
    }
}
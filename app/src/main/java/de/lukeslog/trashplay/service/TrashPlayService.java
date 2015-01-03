package de.lukeslog.trashplay.service;

import android.app.Service;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.player.MusicPlayer;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.radio.RadioManager;
import de.lukeslog.trashplay.statistics.StatisticsCollection;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.SettingsConstants;
import de.lukeslog.trashplay.ui.MainControl;

public class TrashPlayService extends Service {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    private static final String STATE_HEADPHONE = "HeadphoneState";

    private Updater updater;

    public static boolean wifi = false;
    private TrashPlayEventBroadcastReceiver wbr = new TrashPlayEventBroadcastReceiver();
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
        Logger.d(TAG, "SERVICE On Start Command");
        settings = getSharedPreferences(PREFS_NAME, 0);
        ctx=this;
        if (MainControl.activityRunning()) {

            RadioManager.setRadioMode(false);

            registerReceiver();

            NotificationController.createNotification("... running");

            startUpdater();

            startMusicPlayerService();

            registerLocationProvider();

            return START_STICKY;
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    private void registerLocationProvider() {

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {

                StatisticsCollection.newLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);
    }

    private void startMusicPlayerService() {
        startService(new Intent(ctx, MusicPlayer.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "notification should have been shown (main Service)");
    }

    public void stop()
    {
        Logger.d(TAG, "Service player stop");
        if(MainControl.isPlayButtonClicked())
        {
            MusicPlayer.stopeverything();
            MainControl.ctx.finish();
        }
        updater.onPause();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        unregisterAndStopALLTheThings();
        stopUpdater();
        ctx = null;
        Logger.d(TAG, "k thx bye!");
        super.onDestroy();
        //sometimes this stuff does not close even if everything has been shut down. this is a dirty hack to end all dirty hacks.
        //TODO: from time to time I should check if I can live without this
        android.os.Process.killProcess(android.os.Process.myPid());
        crash();
    }

    private void unregisterAndStopALLTheThings() {
        stopForeground(true);
        NotificationController.stopNotification();
        try {
            unregisterReceivers();
        } catch (Exception e) {
            Logger.e(TAG, "small issue while unregistering receivers "+e);
        }
        resetHeadphoneState();
        resetBlueToothState();
        RadioManager.setRadioMode(false);
    }

    @SuppressWarnings("null")
    public void crash() {
        Object o = null;
        o.hashCode();
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.registerReceiver(wbr, intentFilter);
        this.registerReceiver(wbr, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        this.registerReceiver(wbr, new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED));
    }

    private void unregisterReceivers() {
        unregisterReceiver(wbr);
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

    public static boolean serviceRunning() {
        return ctx!=null;
    }

    public void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG);
    }

    private class Updater implements Runnable {

        private Handler handler = new Handler();
        public static final int delay = 5000;
        long counter = 0;

        @Override
        public void run() {
            if(counter%60==10)
            {
                Logger.e(TAG, "ServiceRunner: Time to try to synchronize and Stuff");
                try {
                    boolean lookForNewPlaylists=false;
                    if(counter%180==10){
                        lookForNewPlaylists=true;
                    }
                    MusicCollectionManager.getInstance().syncRemoteStorageWithDevice(lookForNewPlaylists);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            collectStatistics();
            counter++;

            handler.removeCallbacks(this);
            handler.postDelayed(this, delay);
        }

        public void onPause() {
            Logger.d(TAG, "updater on Pause in Service");
            handler.removeCallbacks(this);
        }

        public void onResume() {
            handler.removeCallbacks(this); // remove the old callback
            handler.postDelayed(this, delay); // register a new one
        }
    }

    private void collectStatistics() {
        StatisticsCollection.ping();
    }

    public static TrashPlayService getContext() {
        return ctx;
    }

    class TrashPlayEventBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(TAG, "BROADCAST RECEIVER onReceive");

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
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                Logger.d(TAG, "action_headset_plug");
                processHeadPhoneStateChange(intent);
            }
            if(intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                Logger.d(TAG, "action_sco_audio_state_updated");
                processBlueToothStateChanged(intent);
            }
        }
    }

    private void processBlueToothStateChanged(Intent intent) {
        Logger.d(TAG, "Bt state changed?");
        int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
        Logger.d(TAG, "state="+state);
        SharedPreferences localSettings = getDefaultSettings();
        if(localSettings!=null && state!=BluetoothHeadset.STATE_DISCONNECTING && state!=BluetoothHeadset.STATE_CONNECTING) {
            int oldState = localSettings.getInt("BlueToothState", BluetoothHeadset.STATE_DISCONNECTED);
            Logger.d(TAG, "oldstate: "+oldState);
            if (oldState != state) {
                Logger.d(TAG, "...");
                bluetoothStateHasChanged(state);
            }
        }
    }

    private void bluetoothStateHasChanged(int newState) {
        Logger.d(TAG, "state changed "+newState);
        setBlueToothSettingsState(newState);
        if(newState == BluetoothHeadset.STATE_DISCONNECTED){
            Logger.d(TAG, "is STATE AUDIO DISC");
            if(MusicPlayer.isPlaying()) {
                Logger.d(TAG, "...");
                stopEverything();
            }
        }
    }

    private void resetBlueToothState() {
        setBlueToothSettingsState(-1);
    }

    private void setBlueToothSettingsState(int newState) {
        SharedPreferences.Editor edit = getDefaultSettings().edit();
        edit.putInt("BlueToothState", newState);
        edit.commit();
    }

    private void processHeadPhoneStateChange(Intent intent) {
        int state = intent.getIntExtra("state", -1);
        Logger.d(TAG, "state: "+state);
        SharedPreferences localSettings = getDefaultSettings();
        if(localSettings!=null) {
            Logger.d(TAG, "localsettings!=null");
            int oldState = localSettings.getInt(STATE_HEADPHONE, 0);
            Logger.d(TAG, "oldstate="+oldState);
            if(oldState!=state) {
                Logger.d(TAG, "olol");
                headPhoneStateHasChanged(state);
            }
        }
    }

    private void headPhoneStateHasChanged(int newState) {
        Logger.d(TAG, "---<"+newState);
        switch (newState) {
            case 0:
                Logger.d(TAG, "Headset is unplugged");
                if (MusicPlayer.isPlaying()) {
                    Logger.d(TAG, "MusicPlayer is playing.");
                    setHeadphoneSettings(newState);
                    stopEverything();
                }
                break;
            case 1:
                Logger.d(TAG, "Headset is plugged");
                setHeadphoneSettings(newState);
                break;
            default:
                Logger.d(TAG, "I have no idea what the headset state is");
        }
    }

    private void resetHeadphoneState() {
        setHeadphoneSettings(0);
    }

    private void setHeadphoneSettings(int newState) {
        Logger.d(TAG, "SetHeadphoneSettings: " + newState);
        SharedPreferences.Editor edit = getDefaultSettings().edit();
        edit.putInt(STATE_HEADPHONE, newState);
        edit.commit();
    }

    private void stopEverything(){
        sendBroadcastToStopMusic();
        MainControl.stopUI();
        TrashPlayService.getContext().onDestroy();
    }

    public void sendBroadcastToPause() {
        sendBroadcast(MusicPlayer.ACTION_PAUSE_SONG);
    }

    public void sendBroadcastToGoBack() {
        sendBroadcast(MusicPlayer.ACTION_PREV_SONG);
    }

    public void sendBroadcastToStartNextSong() {
        sendBroadcast(MusicPlayer.ACTION_NEXT_SONG);
    }

    public void sendBroadcastToStartMusic() {
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

    public boolean isInRadioMode() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean radioMode = settings.getBoolean(SettingsConstants.APP_SETTING_RADIO_MODE, false);
        return radioMode;
    }

    public boolean isInTrashMode() {
        SharedPreferences defSetting = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean trashmode = defSetting.getBoolean(SettingsConstants.APP_SETTINGS_TRASHMODE, true);
        return trashmode;
    }
}
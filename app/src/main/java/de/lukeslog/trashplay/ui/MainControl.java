package de.lukeslog.trashplay.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.cloudstorage.GDrive;
import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.cloudstorage.CloudSynchronizationService;
import de.lukeslog.trashplay.cloudstorage.DropBox;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.player.MusicPlayer;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.playlist.SongHelper;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.SettingsConstants;


public class MainControl extends Activity {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    public static Activity ctx;
    private Menu menu = null;
    private int counter = 0;

    public static boolean playButtonClicked = false;

    String displayStringForCurrentTrackInformation = "";

    private UIUpdater uiUpdater;

    ArrayAdapter<String> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_control);

        ctx = this;
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        startService(new Intent(ctx, TrashPlayService.class));

        final ImageView playpause = (ImageView) findViewById(R.id.imageView1);
        final ImageView nextimg = (ImageView) findViewById(R.id.next);
        final ImageView backimg = (ImageView) findViewById(R.id.back);
        ImageView sync = (ImageView) findViewById(R.id.imageView2);
        ImageView wifi = (ImageView) findViewById(R.id.imageView3);
        sync.setVisibility(View.GONE);
        wifi.setVisibility(View.GONE);
        SharedPreferences defSetting = PreferenceManager.getDefaultSharedPreferences(this);
        boolean trashmode = defSetting.getBoolean(SettingsConstants.APP_SETTINGS_TRASHMODE, true);
        if (trashmode) {
            nextimg.setVisibility(View.GONE);
            backimg.setVisibility(View.GONE);
        }
        playpause.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Logger.d(TAG, "click");
                if (TrashPlayService.serviceRunning()) {
                    if (MusicPlayer.getCurrentlyPlayingSong() != null) {
                        sendBroadcastToStopMusic();

                        TrashPlayService.getContext().stop();
                        MainControl.this.finish();
                    } else {
                        sendBroadcastToStartMusic();

                        playpause.setImageResource(R.drawable.pause);
                        setPlayButtonClicked(true);

                        setDisplayStringForCurrentTrackInformation("");
                    }
                }
            }
        });
        nextimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(TAG, "clockkkk");
                sendBroadcastToStartNextSong();
            }
        });
        backimg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Logger.d(TAG, "claaackkkk");
                sendBroadcastToGoBack();
            }
        });
        nextimg.setClickable(true);
        playpause.setClickable(false);
        uiUpdater = new

                UIUpdater();

        uiUpdater.run();
    }

    private void sendBroadcastToPause() {
        Logger.d(TAG, "send Broadcast to Pause");
        sendBroadcast(MusicPlayer.ACTION_PAUSE_SONG);
    }

    private void sendBroadcastToGoBack() {
        Logger.d(TAG, "send Broadcast to go back");
        sendBroadcast(MusicPlayer.ACTION_PREV_SONG);
    }

    private void sendBroadcastToStartNextSong() {
        Logger.d(TAG, "send Broadcast to Start the Next Song");
        sendBroadcast(MusicPlayer.ACTION_NEXT_SONG);
    }

    private void sendBroadcastToStartMusic() {
        Logger.d(TAG, "send Broadcast to Start the Music");
        sendBroadcast(MusicPlayer.ACTION_START_MUSIC);
    }

    private void sendBroadcastToStopMusic() {
        sendBroadcast(MusicPlayer.ACTION_STOP_MUSIC);
    }

    private void sendBroadcast(String action) {
        Logger.d(TAG, "SEND BROADCAST");
        Intent actionIntent = new Intent();
        actionIntent.setAction(action);
        TrashPlayService.getContext().sendBroadcast(actionIntent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        getMenuInflater().inflate(R.menu.main_control, menu);
        this.menu = menu;
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.lastfm) {
                final boolean lastfmactive = settings.getBoolean("scrobble", false);
                if (lastfmactive) {
                    Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogoyes_small);
                    item.setIcon(myIcon);
                } else {
                    Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogono_small);
                    item.setIcon(myIcon);
                }
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        DropBox.authenticate();
        super.onResume();
        uiUpdater.onResume();

    }

    @Override
    protected void onPause() {
        Logger.d(TAG, "on Pause in Activity called");
        super.onPause();
        setPlayButtonClicked(false);
        uiUpdater.onPause();
    }

    @Override
    protected void onDestroy() {
        Logger.d(TAG, "Main On Destroy");
        if (TrashPlayService.serviceRunning()) {
            stopService(new Intent(ctx, TrashPlayService.class));
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        MenuInflater inflater = getMenuInflater();
        this.menu = menu;
        switch (item.getItemId()) {
            case R.id.dropbox:
                if (DropBox.getInstance().isConnected()) {
                    Logger.d(TAG, "click dp");
                    String message = DropBox.getInstance().disconnect();
                    toast(message);
                } else {
                    Logger.d(TAG, "click dp");
                    DropBox.getDropBoxAPI().getSession().startAuthentication(MainControl.this);
                    toast("Connecting to Dropbox");
                }
                return true;
            case R.id.drive:
                if (GDrive.getInstance().isConnected()) {
                    Logger.d(TAG, "click gd");
                    String message = "";
                    toast(message);
                } else {
                    Logger.d(TAG, "click gd");
                    toast("Sorry, GDrive is not implemented yet");
                }
                return true;
            case R.id.lastfm:
                Log.d(TAG, "lastfm...");
                SharedPreferences defsettings = PreferenceManager.getDefaultSharedPreferences(this);
                String lastfmusername = defsettings.getString(SettingsConstants.LASTFM_USER, "");
                String lastfmpassword = defsettings.getString(SettingsConstants.LASTFM_PSW, "");
                if (!lastfmpassword.equals("") && !lastfmusername.equals("")) {
                    boolean scrobbletolastfm = settings.getBoolean("scrobble", false);
                    if (scrobbletolastfm) {
                        Log.d(TAG, "Scrobble was yes will be no");
                        SharedPreferences.Editor edit = settings.edit();
                        edit.putBoolean("scrobble", false);
                        edit.commit();
                    } else {
                        Log.d(TAG, "Scrobble was no will be yes");
                        SharedPreferences.Editor edit = settings.edit();
                        edit.putBoolean("scrobble", true);
                        edit.commit();
                    }
                    updateSettingsIcons();
                }
                return true;
            case R.id.action_settings:
                startSettingsActivity();
                return true;
            case R.id.action_badges:
                toast("sooon...");
                return true;
            case R.id.action_statistics:
                toast("soooon...");
                return true;
            case R.id.action_social:
                toast("Not in this version buddy. I'm not your buddy guy. I'm not your guy, man.");
                return true;
            case R.id.off:
                if (TrashPlayService.serviceRunning()) {
                    TrashPlayService.getContext().stop();
                }
                ctx.stopService(new Intent(ctx, TrashPlayService.class));
                MainControl.this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSettingsIcons() {
        invalidateOptionsMenu();
    }

    private void startSettingsActivity() {
        final Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    public static boolean activityRunning() {
        return ctx != null;
    }

    private void toast(String toastText) {
        Context context = getApplicationContext();
        CharSequence text = toastText;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.d(TAG, "activity result");
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == StorageManager.REQUEST_LINK_TO_DBX) {
                DropBox.authenticate();
                try {
                    MusicCollectionManager.getInstance().syncRemoteStorageWithDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                    toast("There has been an error synchronizing yur data.");
                }
                toast("Now synchronizing with remote Storage. Please Wait");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static boolean isPlayButtonClicked() {
        return playButtonClicked;
    }

    public static void setPlayButtonClicked(boolean playButtonClicked) {
        MainControl.playButtonClicked = playButtonClicked;
    }

    public String getDisplayStringForCurrentTrackInformation() {
        return displayStringForCurrentTrackInformation;
    }

    public void setDisplayStringForCurrentTrackInformation(String displayStringForCurrentTrackInformation) {
        this.displayStringForCurrentTrackInformation = displayStringForCurrentTrackInformation;
    }

    private class UIUpdater implements Runnable {
        private Handler handler = new Handler();
        public static final int delay = 1000;

        @Override
        public void run() {
            if (counter < 0) {
                counter = 0;
            }
            counter++;
            Logger.d(TAG, "UI UPDATER");
            CloudSynchronizationService.updateRegisteredCloudStorageSystems();

            setPrevAndNextButton();

            if (CloudSynchronizationService.atLeastOneCloudStorageServiceIsConnected() && MusicCollectionManager.getInstance().collectionNotEmpty()) {
                setButtonToPlayButton();
            } else if (MusicPlayer.getCurrentlyPlayingSong() != null) {
                setButtonToStopButton();
            }
            if (menu != null) {
                Logger.d(TAG, "menu is not null");
                setMenuIcons();
            }

            setWifiIcon();

            setSyncInProgressAnimation();

            setShiftedSongTitle();

            setPlayTimes();

            fillListOfNextSongs();

            fillInfoBox();

            handler.removeCallbacks(this); // remove the old callback
            handler.postDelayed(this, delay); // register a new one
        }

        public void onPause() {
            Logger.d(TAG, "Activity update on Pause");
            handler.removeCallbacks(this); // stop the map from updating
        }

        public void onResume() {
            handler.removeCallbacks(this); // remove the old callback
            handler.postDelayed(this, delay); // register a new one
        }
    }

    private void setButtonToStopButton() {
        ImageView playpause = (ImageView) findViewById(R.id.imageView1);
        if (!playpause.isClickable()) {
            playpause.setImageResource(R.drawable.pause);
            playpause.setClickable(true);

        }
    }


    private void setButtonToPlayButton() {
        ImageView playpause = (ImageView) findViewById(R.id.imageView1);
        if (!playpause.isClickable()) {
            playpause.setImageResource(R.drawable.play);
            playpause.setClickable(true);

        }
    }

    private void setWifiIcon() {
        if (TrashPlayService.serviceRunning()) {
            boolean wificonnected = TrashPlayService.wifi;
            ImageView wifi = (ImageView) findViewById(R.id.imageView3);
            if (wificonnected) {
                wifi.setVisibility(View.VISIBLE);
            } else {
                wifi.setVisibility(View.GONE);
            }
        }
    }

    private void setPrevAndNextButton() {
        final ImageView nextimg = (ImageView) findViewById(R.id.next);
        final ImageView backimg = (ImageView) findViewById(R.id.back);
        SharedPreferences defSetting = PreferenceManager.getDefaultSharedPreferences(this);
        boolean trashmode = defSetting.getBoolean(SettingsConstants.APP_SETTINGS_TRASHMODE, true);
        if (trashmode) {
            nextimg.setVisibility(View.GONE);
            backimg.setVisibility(View.GONE);
        } else {
            nextimg.setVisibility(View.VISIBLE);
            backimg.setVisibility(View.VISIBLE);
        }
    }

    private void fillInfoBox() {
        TextView infoBox = (TextView) findViewById(R.id.infoBox);
        if (MusicPlayer.getCurrentlyPlayingSong() != null) {
            infoBox.setText("Up Next");
        } else if (MusicCollectionManager.getInstance().collectionNotEmpty()) {
            infoBox.setText("Press Play To Start");
        } else if (!CloudSynchronizationService.atLeastOneCloudStorageServiceIsConnected()) {
            infoBox.setText("Not connected to any storage with Playlists");
        } else if (MusicCollectionManager.getInstance().getNumberOfViableSongs() == 0) {
            infoBox.setText("No songs");
        }
    }

    private void setPlayTimes() {
        TextView position = (TextView) findViewById(R.id.posi);
        String posix = MusicPlayer.playPosition();
        String length = MusicPlayer.playLength();
        if (length.equals("") || posix.equals("")) {
            position.setText("(" + MusicCollectionManager.getInstance().getNumberOfViableSongs() + ")");

        } else {
            position.setText("(" + MusicCollectionManager.getInstance().getNumberOfViableSongs() + ")" + "[" + posix + "|" + length + "]");
        }
    }

    private void setSyncInProgressAnimation() {
        if (StorageManager.syncInProgress) {
            ImageView sync = (ImageView) findViewById(R.id.imageView2);
            if (counter % 4 == 0) {
                sync.setImageResource(R.drawable.synchronize1);
            }
            if (counter % 4 == 1) {
                sync.setImageResource(R.drawable.synchronize2);
            }
            if (counter % 4 == 2) {//TODO: more synchronize grafics
                sync.setImageResource(R.drawable.synchronize1);
            }
            if (counter % 4 == 3) {
                sync.setImageResource(R.drawable.synchronize2);
            }
            sync.setVisibility(View.VISIBLE);
        } else {
            ImageView sync = (ImageView) findViewById(R.id.imageView2);
            sync.setVisibility(View.GONE);
        }
    }

    private void setShiftedSongTitle() {
        //TODO: this method does not perform correctly. Please write some unit tests.
        Song currentlyPlayingSong = MusicPlayer.getCurrentlyPlayingSong();
        if (currentlyPlayingSong != null) {
            String newdisplay = "";
            setDisplayStringForCurrentTrackInformation(SongHelper.getTitleInfoAsString(currentlyPlayingSong));
            newdisplay = SongHelper.getTitleInfoAsString(currentlyPlayingSong);
            int shift = counter % newdisplay.length();
            //Logger.d(TAG, "shift: "+shift);
            CharSequence d1 = newdisplay.subSequence(0, shift);
            String s1Str = d1.toString();
            CharSequence d2 = newdisplay.subSequence(shift, newdisplay.length());
            String s2Str = d2.toString();
            String ndx = s2Str + "+++" + s1Str;
            TextView textView1 = (TextView) findViewById(R.id.songinfo);
            textView1.setText(ndx);
        }
    }

    private void fillListOfNextSongs() {
        final List<Song> listItems = MusicCollectionManager.getInstance().getListOfNextSongs();
        if (listItems.size() > 0) {
            final List<String> spinnerArray = new ArrayList<String>();
            for (Song song : listItems) {
                spinnerArray.add(SongHelper.getTitleInfoAsStringWithPlayCount(song));
            }
            String s = "";
            if(MusicPlayer.getCurrentlyPlayingSong()!=null) {
                s = spinnerArray.remove(0);
            }
            if (s != null && spinnerArray.get(0)!=null) {
                adapter = new ArrayAdapter<String>(MainControl.this, android.R.layout.simple_spinner_item, spinnerArray);
                ListView lv = (ListView) findViewById(R.id.nextsongs);
                lv.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void setMenuIcons() {
        for (StorageManager storageManager : CloudSynchronizationService.getRegisteredCloudStorageServices()) {
            Logger.d(TAG, "->");
            adjustMenuIconForCloudStorageService(storageManager);
        }

    }

    private void adjustMenuIconForCloudStorageService(StorageManager storageManager) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == storageManager.menuItem()) {
                if (storageManager.isConnected()) {
                    Drawable myIcon = getResources().getDrawable(storageManager.getIconResourceConnected());
                    item.setIcon(myIcon);
                } else {
                    Drawable myIcon = getResources().getDrawable(storageManager.getIconResourceNotConnected());
                    item.setIcon(myIcon);
                }
            }
        }
    }
}

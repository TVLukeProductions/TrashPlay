package de.lukeslog.trashplay.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.cloudstorage.CloudSynchronizationService;
import de.lukeslog.trashplay.cloudstorage.DropBox;
import de.lukeslog.trashplay.cloudstorage.GDrive;
import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.player.MusicPlayer;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.playlist.PlayListHelper;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.playlist.SongHelper;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.SettingsConstants;


public class MainControl extends Activity {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    public static MainControl ctx;
    SongListAdapter songlistadapter;
    private Menu menu = null;
    private int counter = 0;
    List<Song> oldsonglist = new ArrayList<Song>();

    public static boolean playButtonClicked = false;
    public static boolean authenticatingDropBox = true;

    String displayStringForCurrentTrackInformation = "";

    private UIUpdater uiUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "on Create");
        ActiveAndroid.initialize(this);
        setContentView(R.layout.activity_main_control);

        ctx = this;
        startService(new Intent(ctx, TrashPlayService.class));

        final ImageView playpause = (ImageView) findViewById(R.id.imageView1);
        final ImageView nextimg = (ImageView) findViewById(R.id.next);
        final ImageView backimg = (ImageView) findViewById(R.id.back);
        ImageView sync = (ImageView) findViewById(R.id.imageView2);
        ImageView wifi = (ImageView) findViewById(R.id.imageView3);
        sync.setImageResource(IconHelper.getIcon(IconHelper.ICON_SYNC_INVISIBLE, 0));
        wifi.setImageResource(IconHelper.getIcon(IconHelper.ICON_WIFI_INVISIBLE, 0));
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
                        TrashPlayService.getContext().sendBroadcastToStopMusic();

                        TrashPlayService.getContext().stop();
                        MainControl.this.finish();
                    } else {
                        TrashPlayService.getContext().sendBroadcastToStartMusic();

                        playpause.setImageResource(IconHelper.getIcon(IconHelper.ICON_STOP, 0));
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
                TrashPlayService.getContext().sendBroadcastToStartNextSong();
            }
        });
        backimg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Logger.d(TAG, "claaackkkk");
                TrashPlayService.getContext().sendBroadcastToGoBack();
            }
        });
        nextimg.setClickable(true);
        playpause.setClickable(false);
        uiUpdater = new UIUpdater();

        uiUpdater.run();
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
                    Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogono_small);
                    item.setIcon(myIcon);
                } else {
                    Drawable myIcon = getResources().getDrawable(R.drawable.lastfmlogoyes_small);
                    item.setIcon(myIcon);
                }
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        Logger.d(TAG, "onResume");
        uiUpdater.onResume();
        DropBox.authenticate();
        if (authenticatingDropBox) {
            authenticatingDropBox = false;
            MusicCollectionManager.getInstance().syncRemoteStorageWithDevice(true);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "on Pause in Activity called");
        setPlayButtonClicked(false);
        uiUpdater.onPause();
    }

    @Override
    protected void onDestroy() {
        Logger.d(TAG, "Main On Destroy");
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
                    authenticatingDropBox = true;
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
                Logger.d(TAG, "lastfm...");
                SharedPreferences defsettings = PreferenceManager.getDefaultSharedPreferences(this);
                String lastfmusername = defsettings.getString(SettingsConstants.LASTFM_USER, "");
                String lastfmpassword = defsettings.getString(SettingsConstants.LASTFM_PSW, "");
                if (!lastfmpassword.equals("") && !lastfmusername.equals("")) {
                    boolean scrobbletolastfm = settings.getBoolean("scrobble", false);
                    if (scrobbletolastfm) {
                        Logger.d(TAG, "Scrobble was yes will be no");
                        SharedPreferences.Editor edit = settings.edit();
                        edit.putBoolean("scrobble", false);
                        edit.commit();
                    } else {
                        Logger.d(TAG, "Scrobble was no will be yes");
                        SharedPreferences.Editor edit = settings.edit();
                        edit.putBoolean("scrobble", true);
                        edit.commit();
                    }
                    updateSettingsIcons();
                } else {
                    toast("You need to provide your last.fm data to activate scrobbling to your personal account.");
                }
                return true;
            case R.id.action_settings:
                startSettingsActivity();
                return true;
            case R.id.action_badges:
                toast("Not yet implemented.");
                //Intent i1 = new Intent(this, Badges.class);
                //startActivity(i1);
                return true;
            case R.id.action_statistics:
                Intent i = new Intent(this, StatisticsActivity.class);
                startActivity(i);
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
        final Intent intent = new Intent(this, SettingsActivity.class);
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
        public static final int delay = 500;

        @Override
        public void run() {
            if (counter < 0) {
                counter = 0;
            }
            counter++;
            CloudSynchronizationService.updateRegisteredCloudStorageSystems();

            setPrevAndNextButton();

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
            boolean listenalong = settings.getBoolean("listenalong", false);
            if (MusicPlayer.getCurrentlyPlayingSong() != null) {
                setButtonToStopButton();
            } else if (listenalong && MusicCollectionManager.getInstance().radiofile != null && MusicPlayer.getCurrentlyPlayingSong() != null) {
                setButtonToStopButton();
                ImageView playpause = (ImageView) findViewById(R.id.imageView1);
                playpause.setClickable(true);

            } else if (MusicCollectionManager.getInstance().collectionGreater10()) {
                setButtonToPlayButton();
            }
            if (menu != null) {
                setMenuIcons();
            }

            setWifiIcon();

            setRadioIcon();

            setSyncInProgressAnimation();

            setSongTitleAndArtist();

            setPlayTimes();

            fillListOfNextSongs();

            fillInfoBox();

            setProgressBar();

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

    private void setProgressBar() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
        double posix = MusicPlayer.playPosition();
        double length = MusicPlayer.playLength();
        if(length>0.0) {
            progress.setProgress((int) (posix / (length / 100.0)));
        }
    }

    private void setRadioIcon() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean radioMode = settings.getBoolean(SettingsConstants.APP_SETTING_RADIO_MODE, false);
        ImageView radio = (ImageView) findViewById(R.id.radioimg);
        if (radioMode) {
            radio.setVisibility(View.VISIBLE);
            if(MusicPlayer.getCurrentlyPlayingSong()==null) {
                radio.setImageResource(IconHelper.getIcon(IconHelper.ICON_RADIO, 0));
            }
            radio.setImageResource(IconHelper.getIcon(IconHelper.ICON_RADIO, counter));
        } else {
            radio.setImageResource(IconHelper.getIcon(IconHelper.ICON_RADIO_INVISIBLE, 0));
        }
    }

    private void setButtonToStopButton() {
        ImageView playpause = (ImageView) findViewById(R.id.imageView1);
        DateTime now = new DateTime();
        if(now.getMonthOfYear()==12 && now.getDayOfMonth()>20 && now.getDayOfMonth()<27){
            playpause.setImageResource(IconHelper.getIcon(IconHelper.SANTA, counter));
        } else {
            playpause.setImageResource(IconHelper.getIcon(IconHelper.ICON_STOP, 0));
        }
        playpause.setClickable(true);
    }


    private void setButtonToPlayButton() {
        ImageView playpause = (ImageView) findViewById(R.id.imageView1);
        playpause.setImageResource(IconHelper.getIcon(IconHelper.ICON_PLAY, 0));
        if (!playpause.isClickable()) {
            playpause.setClickable(true);

        }
    }

    private void setWifiIcon() {
        if (TrashPlayService.serviceRunning()) {
            boolean wificonnected = TrashPlayService.wifi;
            ImageView wifi = (ImageView) findViewById(R.id.imageView3);
            if (wificonnected) {
                wifi.setImageResource(IconHelper.getIcon(IconHelper.ICON_WIFI, 0));
            } else {
                wifi.setImageResource(IconHelper.getIcon(IconHelper.ICON_WIFI_INVISIBLE, 0));
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
            infoBox.setVisibility(View.GONE);
        } else if (MusicCollectionManager.getInstance().collectionNotEmpty()) {
            infoBox.setVisibility(View.VISIBLE);
            infoBox.setText("Press Play To Start");
        } else if (!CloudSynchronizationService.atLeastOneCloudStorageServiceIsConnected()) {
            infoBox.setVisibility(View.VISIBLE);
            infoBox.setText("Not connected to any storage with Playlists");
        } else if (SongHelper.getNumberOfViableSongs() == 0) {
            infoBox.setVisibility(View.VISIBLE);
            infoBox.setText("No songs");
        }
    }

    private void setPlayTimes() {
        TextView position = (TextView) findViewById(R.id.posi);
        TextView positionSingle = (TextView) findViewById(R.id.posisingle);
        TextView lengthSingle = (TextView) findViewById(R.id.lengthsingle);
        String posix = MusicPlayer.playPositionAsTimeString();
        String length = MusicPlayer.playLengthAsTimeString();
        if (length.equals("") || posix.equals("")) {
            position.setText("(" + SongHelper.getNumberOfViableSongs() + ")");

        } else {
            position.setText("(" + SongHelper.getNumberOfViableSongs() + ")");
        }
        positionSingle.setText(posix);
        lengthSingle.setText(length);
    }

    private void setSyncInProgressAnimation() {
        //if (StorageManager.syncInProgress || PlayListHelper.sync) {
        if (StorageManager.syncInProgress) {
            ImageView sync = (ImageView) findViewById(R.id.imageView2);
            sync.setImageResource(IconHelper.getIcon(IconHelper.ICON_SYNC, counter));
            sync.setVisibility(View.VISIBLE);
        } else {
            ImageView sync = (ImageView) findViewById(R.id.imageView2);
            sync.setImageResource(IconHelper.getIcon(IconHelper.ICON_SYNC_INVISIBLE, 0));
        }
    }

    private void setSongTitleAndArtist() {
        Song currentlyPlayingSong = MusicPlayer.getCurrentlyPlayingSong();
        if (currentlyPlayingSong != null) {
            TextView songInfo= (TextView) findViewById(R.id.songinfo);
            TextView artistInfo= (TextView) findViewById(R.id.artistinfo);
            songInfo.setText(currentlyPlayingSong.getSongName());
            artistInfo.setText(currentlyPlayingSong.getArtist());
        }
    }

    private void fillListOfNextSongs() {

        List<Song> songList = new ArrayList<Song>(MusicCollectionManager.getInstance().getListOfNextSongs());
        if(MusicPlayer.getCurrentlyPlayingSong()!=null) {
            songList.remove(0);
        }
        if(!songList.equals(oldsonglist)) {
            oldsonglist = songList;
            ListView listViewWithAlarms = (ListView) findViewById(R.id.nextsongs);
            songlistadapter = new SongListAdapter(ctx, songList);
            listViewWithAlarms.setAdapter(songlistadapter);
            songlistadapter.notifyDataSetChanged();
            listViewWithAlarms.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    Logger.d(TAG, "longclick");
                    return true;
                }
            });
        }
    }

    private void setMenuIcons() {
        for (StorageManager storageManager : CloudSynchronizationService.getRegisteredCloudStorageServices()) {
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

    public static void stopUI(){
        if(ctx!=null){
            ctx.onDestroy();
        }
    }
}

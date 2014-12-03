package de.lukeslog.trashplay.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.session.AccessTokenPair;

import java.io.File;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.cloudstorage.CloudStorage;
import de.lukeslog.trashplay.cloudstorage.CloudSynchronizationService;
import de.lukeslog.trashplay.cloudstorage.DropBox;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.player.MusicPlayer;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;


public class MainControl extends Activity {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    public static Activity ctx;
    private Menu menu = null;
    private int counter = 0;

    public static boolean playButtonClicked = false;

    String displayStringForCurrentTrackInformation = "";

    private UIUpdater uiUpdater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_control);

        ctx = this;
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        startService(new Intent(ctx, TrashPlayService.class));

        final ImageView playpause = (ImageView) findViewById(R.id.imageView1);
        ImageView sync = (ImageView) findViewById(R.id.imageView2);
        ImageView wifi = (ImageView) findViewById(R.id.imageView3);
        sync.setVisibility(View.GONE);
        wifi.setVisibility(View.GONE);
        playpause.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "click");
                if (TrashPlayService.serviceRunning()) {
                    if (MusicPlayer.getCurrentlyPlayingSong() != null) {
                        sendBroadcastToStopMusic();

                        TrashPlayService.getContext().stop();
                        MainControl.this.finish();
                    } else {
                        //TODO: Broadcast to the thing that it shukld play some funky music
                        sendBroadcastToStartMusic();

                        playpause.setImageResource(R.drawable.pause);
                        setPlayButtonClicked(true);

                        setDisplayStringForCurrentTrackInformation("");
                    }
                }
            }
        });
        playpause.setClickable(false);
        uiUpdater = new

                UIUpdater();

        uiUpdater.run();
    }

    private void sendBroadcastToStartMusic() {
        Log.d(TAG, "send Broadcast to Start the Music");
        sendBroadcast(MusicPlayer.ACTION_START_MUSIC);
    }

    private void sendBroadcastToStopMusic() {
        sendBroadcast(MusicPlayer.ACTION_STOP_MUSIC);
    }

    private void sendBroadcast(String action) {
        Log.d(TAG, "SEND BROADCAST");
        Intent actionIntent = new Intent();
        actionIntent.setAction(action);
        //startmusic.putExtra("AmbientActionID", ctx.getActionID());
        TrashPlayService.getContext().sendBroadcast(actionIntent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_control, menu);
        this.menu = menu;
        return true;
    }

    @Override
    protected void onResume() {
        DropBox.authenticate();
        super.onResume();
        uiUpdater.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "on Pause in Activity called");
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
                    Log.d(TAG, "click dp");
                    String message = DropBox.getInstance().disconnect();
                    toast(message);
                } else {
                    Log.d(TAG, "click dp");
                    DropBox.getDropBoxAPI().getSession().startAuthentication(MainControl.this);
                    toast("Connecting to Dropbox");
                }
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
        Log.d(TAG, "activity result");
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CloudStorage.REQUEST_LINK_TO_DBX) {
                DropBox.authenticate();
                try {
                    MusicCollectionManager.getInstance().syncRemoteStorageWithDevice();
                } catch (Exception e) {
                    e.printStackTrace(); //TODO generate cool stuff
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
            Log.d(TAG, "UI UPDATER");
            CloudSynchronizationService.updateRegisteredCloudStorageSystems();

            if (CloudSynchronizationService.atLeastOneCloudStorageServiceIsConnected() && MusicCollectionManager.getInstance().collectionNotEmpty()) {
                setButtonToPlayButton();
            }
            if (menu != null) {
                Log.d(TAG, "menu is not null");
                setMenuIcons();
            }
            setSyncInProgressAnimation();

            setShiftedSongTitle();

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

    private void setSyncInProgressAnimation() {
        if (CloudStorage.syncInProgress) {
            ImageView sync= (ImageView) findViewById(R.id.imageView2);
            if(counter%4==0) {
                sync.setImageResource(R.drawable.synchronize1);
            }
            if(counter%4==1){
                sync.setImageResource(R.drawable.synchronize2);
            }
            if(counter%4==2){//TODO: more synchronize grafics
                sync.setImageResource(R.drawable.synchronize1);
            }
            if(counter%4==3){
                sync.setImageResource(R.drawable.synchronize2);            }
            sync.setVisibility(View.VISIBLE);
        } else {
            ImageView sync = (ImageView) findViewById(R.id.imageView2);
            sync.setVisibility(View.GONE);
        }
    }

    private void setShiftedSongTitle() {
        //shif stuff in the field for title
        Song currentlyPlayingSong = MusicPlayer.getCurrentlyPlayingSong();
        if (currentlyPlayingSong != null) {
            String newdisplay = "";
            setDisplayStringForCurrentTrackInformation(currentlyPlayingSong.getTitleInfoAsString());
            newdisplay = currentlyPlayingSong.getTitleInfoAsString();
            int shift = counter % newdisplay.length();
            //Log.d(TAG, "shift: "+shift);
            CharSequence d1 = newdisplay.subSequence(0, shift);
            String s1Str = d1.toString();
            CharSequence d2 = newdisplay.subSequence(shift, newdisplay.length());
            String s2Str = d2.toString();
            String ndx = s2Str + "+++" + s1Str;
            TextView textView1 = (TextView) findViewById(R.id.songinfo);
            textView1.setText(ndx);
        }
    }

    private void setMenuIcons() {
        Log.d(TAG, "setMenuIcons");
        for (CloudStorage cloudStorage : CloudSynchronizationService.getRegisteredCloudStorageServices()) {
            Log.d(TAG, "->");
            adjustMenuIconForCloudStorageService(cloudStorage);
        }

    }

    private void adjustMenuIconForCloudStorageService(CloudStorage cloudStorage) {
        Log.d(TAG, "adjust Menu Icons");
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == cloudStorage.menuItem()) {
                if (cloudStorage.isConnected()) {
                    Drawable myIcon = getResources().getDrawable(cloudStorage.getIconResourceConnected());
                    item.setIcon(myIcon);
                } else {
                    Drawable myIcon = getResources().getDrawable(cloudStorage.getIconResouceNotConnected());
                    item.setIcon(myIcon);
                }
            }
        }
    }


    private void setButtonToPlayButton() {
        ImageView playpause = (ImageView) findViewById(R.id.imageView1);
        if (!playpause.isClickable()) {
            playpause.setImageResource(R.drawable.play);
            //playpause.setVisibility(View.VISIBLE);
            playpause.setClickable(true);

        }
    }
}

package de.lukeslog.trashplay.player;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;

import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;

import java.io.File;
import java.io.IOException;

import de.lukeslog.trashplay.cloudstorage.CloudStorage;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.lastfm.TrashPlayLastFM;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.support.Logger;

/**
 * Created by lukas on 25.04.14.
 */
public class MusicPlayer extends Service implements OnPreparedListener, OnCompletionListener, MediaPlayer.OnErrorListener {
    public static final String ACTION_START_MUSIC = "startmusic";
    public static final String ACTION_STOP_MUSIC = "stopmusic";

    public static final String TAG = TrashPlayConstants.TAG;

    private static Service ctx;
    private static Song currentlyPlayingSong = null;

    static MediaPlayer mp;

    String actionID = "";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //Log.d(TAG, "ClockWorkService onStartCommand()");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mp = null;
        ctx=this;
        registerIntentFilters();
    }

    private void registerIntentFilters() {
        IntentFilter inf = new IntentFilter(ACTION_START_MUSIC);
        IntentFilter inf2 = new IntentFilter(ACTION_STOP_MUSIC);
        registerReceiver(mReceiver, inf);
        registerReceiver(mReceiver, inf2);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mp = null;
    }

    private void playmp3(Song song) {
        boolean mExternalStorageAvailable = false;
        String state = Environment.getExternalStorageState();
        Logger.d(TAG, "Go Play 3");
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = false;
        }
        if (mExternalStorageAvailable) {
            File file = new File(CloudStorage.LOCAL_STORAGE + song.getFileName());

            scrobbleTrack(file);

            try {
                playMusic(file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void scrobbleTrack(File file) {
        String artist = "";
        String title = "";
        try {
            MP3File mp3 = new MP3File(file);
            ID3v1 id3 = mp3.getID3v1Tag();
            artist = id3.getArtist();
            //Log.d(TAG, "----------->ARTIST:" + artist);
            title = id3.getSongTitle();
            //Log.d(TAG, "----------->SONG:" + song);
            //TODO: Scrobble to trashplay and private account
            TrashPlayLastFM.scrobble(artist, title);
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (TagException e1) {
            e1.printStackTrace();
        } catch (Exception ex) {
            //Log.e(TAG, "There has been an exception while extracting ID3 Tag Information from the MP3");
        }
    }

    private void playMusic(String musicpath) throws IOException {
        mp = new MediaPlayer();
        mp.setDataSource(musicpath);
        mp.setLooping(false);
        mp.setVolume(0.99f, 0.99f);
        Logger.d(TAG, "...");
        mp.setOnCompletionListener(this);
        mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        Logger.d(TAG, "....");
        mp.setOnPreparedListener(this);
        Logger.d(TAG, ".....");
        mp.prepareAsync();
    }

    public void stop() {
        Logger.d(TAG, "stop Media Player Service");
        mp.stop();
        mp.release();
        mp = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mpx) {
        Logger.d(TAG, "on Prepared!");
        mpx.setOnCompletionListener(this);
        Logger.d(TAG, "ok, I'v set the on Completion Listener again...");
        mpx.start();
    }

    @Override
    public void onCompletion(MediaPlayer mpx) {
        Logger.d(TAG, "on Completetion");
        playmp3(MusicCollectionManager.getInstance().getNextSong());
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Logger.d(TAG, "MEDIAPLAYER ON ERROR");
        playmp3(MusicCollectionManager.getInstance().getNextSong());
        return true;
    }

    public static void stopeverything() {
        if (mp != null) {
            mp.stop();
            mp.release();
            mp = null;
        }
        if(MusicPlayer.ctx != null) {
            ctx.stopSelf();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_START_MUSIC)) {
                Logger.d(TAG, "I GOT THE START MUSIC THING!");
                // actionID = intent.getStringExtra("AmbientActionID");
                Song nextSong = MusicCollectionManager.getInstance().getNextSong();
                playmp3(nextSong);
            }
            if (action.equals(ACTION_STOP_MUSIC)) {
                Logger.d(TAG, "STOooooooooP");
                //String newactionID = intent.getStringExtra("AmbientActionID");
                stop();
                actionID = "";
            }
        }
    };
}

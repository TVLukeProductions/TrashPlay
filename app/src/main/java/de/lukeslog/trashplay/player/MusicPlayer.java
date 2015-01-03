package de.lukeslog.trashplay.player;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.lastfm.PersonalLastFM;
import de.lukeslog.trashplay.lastfm.TrashPlayLastFM;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.playlist.SongHelper;
import de.lukeslog.trashplay.service.NotificationController;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.TrashPlayUtils;

public class MusicPlayer extends Service implements OnPreparedListener, OnCompletionListener, MediaPlayer.OnErrorListener {

    public static final String ACTION_START_MUSIC = "trashplay_startmusic";
    public static final String ACTION_STOP_MUSIC = "trashplay_stopmusic";
    public static final String ACTION_NEXT_SONG = "trashplay_nextSong";
    public static final String ACTION_PREV_SONG = "trashplay_prevSong";
    public static final String ACTION_PAUSE_SONG = "trashplay_pauseSong";

    public static final String TAG = TrashPlayConstants.TAG;


    private static Service ctx;
    private static Song currentlyPlayingSong = null;

    private static long timeStampAtLastPlayOrCompletion = 0;

    private String title = "";
    private String artist = "";

    private static MediaPlayer mp;
    private AudioManager am;
    private float currentVolume = 15.0f;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //Logger.d(TAG, "ClockWorkService onStartCommand()");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mp = null;
        ctx = this;
        registerIntentFilters();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mp = null;
    }

    private void playmp3(Song song) {
        timeStampAtLastPlayOrCompletion = new DateTime().getMillis();
        setCurrentlyPlayingSong(song);
        if (song == null) {
            Logger.d(TAG + "_MEDIAPLAYER", "playmp3 got null");
            TrashPlayService.getContext().toast("Something went wrong.");
            stop();
        } else {
            boolean mExternalStorageAvailable = false;
            String state = Environment.getExternalStorageState();
            Logger.d(TAG + "_MEDIAPLAYER", "Go Play 3");
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
                setTrackInfo(song);
                try {
                    playMusic(song);
                } catch (IOException e) {
                    Logger.e(TAG + "_MEDIAPLAYER", "error1 in MusicPlayer");
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.e(TAG + "_MEDIAPLAYER", "error2 in MusicPlayer");
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean needToScrobble() {
        if(artist!=null && title!=null) {
            return (!artist.equals("") && !title.equals(""));
        }
        return false;
    }

    private void setTrackInfo(Song song) {
            SongHelper.getMetaData(song);
            artist = song.getArtist();
            title = song.getSongName();
            NotificationController.createNotification(title, artist);
    }

    private void scrobbleTrack() {
        Logger.d(TAG + "_MEDIAPLAYER", "scrobble....");
        if(artist!=null && title!=null) {
            TrashPlayLastFM.scrobble(artist, title);
            if (TrashPlayService.serviceRunning()) {
                Logger.d(TAG + "_MEDIAPLAYER", "Service Running...");
                PersonalLastFM.scrobble(artist, title, TrashPlayService.getContext().settings);
            }
        }
    }

    private void playMusic(Song song) throws Exception {
        try {
            File file = getFileFromSong(song);
            String musicPath = file.getAbsolutePath();
            mp = new MediaPlayer();
            mp.setDataSource(musicPath);
            mp.setLooping(false);
            mp.setOnCompletionListener(this);
            mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            Logger.d(TAG, "....");
            mp.setOnPreparedListener(this);
            Logger.d(TAG, ".....");
            mp.prepareAsync();
        } catch (IllegalStateException e) {
            Logger.d(TAG + "_MEDIAPLAYER", "illegalStateException");
            mp = null;
            playmp3(MusicCollectionManager.getInstance().getNextSong());
        }
    }

    private File getFileFromSong(Song song) throws IOException {
        if (song != null) {
            return new File(StorageManager.LOCAL_STORAGE + song.getFileName());
        }
        throw new IOException();
    }

    public void stop() {
        Logger.d(TAG + "_MEDIAPLAYER", "stop Media Player Service");
        try {
            mp.stop();
        } catch (Exception e) {

        }
        try {
            mp.release();
        } catch (Exception e) {

        }
        setCurrentlyPlayingSong(null);
        mp = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mpx) {
        Logger.d(TAG + "_MEDIAPLAYER", "on Prepared!");
        mpx.setOnCompletionListener(this);
        int duration = mpx.getDuration();
        if (duration > 0) {
            if (getCurrentlyPlayingSong().getDuration() != duration) {
                resetSongDurationOnPreperation(duration);
            }
            seekTimeToStart(mpx, duration);
        }
        Logger.d(TAG + "_MEDIAPLAYER", "ok, I'v set the on Completion Listener again...");
        try {
            mpx.start();
        } catch (Exception e) {
            Logger.e(TAG + "_MEDIAPLAYER", "ERROR " + e);
        }
    }

    private void seekTimeToStart(MediaPlayer mpx, int duration) {
        if (setDiferentTimeToStart(duration)) {
            Logger.d(TAG + "_MEDIAPLAYER", "seek" + (int) MusicCollectionManager.timeDifInMillis);
            if (MusicCollectionManager.timeDifInMillis > 5000) {
                mpx.seekTo((int) MusicCollectionManager.timeDifInMillis - 5000);
            } else {
                mpx.seekTo((int) MusicCollectionManager.timeDifInMillis);
            }
        }
        Logger.d(TAG + "_MEDIAPLAYER", "continue");
    }

    private boolean setDiferentTimeToStart(int duration) {
        TrashPlayService.getContext().toast("time dif " + MusicCollectionManager.timeDifInMillis);
        Logger.d(TAG + "_MEDIAPLAYER", "timedifstuff");
        boolean c1 = MusicCollectionManager.timeDifInMillis > 0;
        Logger.d(TAG + "_MEDIAPLAYER", "a");
        boolean c2 = MusicCollectionManager.timeDifInMillis < duration;
        boolean c3 = MusicCollectionManager.timeDifInMillis < 30000;
        return (c1 && c2 && c3);
    }

    private void resetSongDurationOnPreperation(int duration) {
        SongHelper.setDuration(getCurrentlyPlayingSong(), duration);
        try {
            MusicCollectionManager.getInstance().updateRadioFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mpx) {
        Song lastsong = getCurrentlyPlayingSong();
        long now = new DateTime().getMillis();
        Logger.d(TAG + "_MEDIAPLAYER", "MEDIAPLAYER: on Completetion!");

        Logger.d(TAG + "_MEDIAPLAYER", "" + (now - timeStampAtLastPlayOrCompletion));
        if ((now - timeStampAtLastPlayOrCompletion) > 2000 || lastsong.getDuration()<2000) {
            Logger.d(TAG, "ok.. we are don with this song... probably");
            if (needToScrobble()) {
                scrobbleTrack();
            }
            MusicCollectionManager.getInstance().finishedSong();
            stop();
            try {
                playmp3(MusicCollectionManager.getInstance().getNextSong());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Logger.d(TAG + "_MEDIAPLAYER", "I don't think we played that song yet");
            stop();
            playmp3(lastsong);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Logger.d(TAG + "_MEDIAPLAYER", "MEDIAPLAYER ON ERROR");
        stop();
        try {
            if (getCurrentlyPlayingSong() != null) {
                if (getCurrentlyPlayingSong().isInActiveUse()) {
                    playmp3(getCurrentlyPlayingSong());
                }
                return true;
            } else {
                playmp3(MusicCollectionManager.getInstance().getNextSong());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void stopeverything() {
        if (mp != null) {
            mp.stop();
            mp.release();
            mp = null;
        }
        if (MusicPlayer.ctx != null) {
            ctx.stopSelf();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_START_MUSIC)) {
                Logger.d(TAG + "_MEDIAPLAYER", "I GOT THE START MUSIC THING!");
                // actionID = intent.getStringExtra("AmbientActionID");
                requestAudioFocus();
            }
            if (action.equals(ACTION_STOP_MUSIC)) {
                Logger.d(TAG + "_MEDIAPLAYER", "STOooooooooP");
                //String newactionID = intent.getStringExtra("AmbientActionID");
                stop();
                if (am != null) {
                    am.abandonAudioFocus(afChangeListener);
                }
            }
            if (action.equals(ACTION_NEXT_SONG)) {
                Logger.d(TAG + "_MEDIAPLAYER", "NEXT SONG REQUESTED");
                if (currentlyPlayingSong != null) {
                    MusicCollectionManager.getInstance().finishedSong();
                    stop();
                    Song nextSong = null;
                    try {
                        nextSong = MusicCollectionManager.getInstance().getNextSong();
                        Logger.d(TAG + "_MEDIAPLAYER", "nextsong?");
                    } catch (Exception e) {
                        Logger.e(TAG + "_MEDIAPLAYER", "Error while getting next song " + e);
                    }
                    playmp3(nextSong);
                }
            }
            if (action.equals(ACTION_PREV_SONG)) {
                Logger.d(TAG + "_MEDIAPLAYER", "PREVIOUS SONG REQUESTED");
                if (currentlyPlayingSong != null && mp != null) {
                    Logger.d(TAG + "_MEDIAPLAYER", "POSITION: " + mp.getCurrentPosition());
                    if (mp.getCurrentPosition() > 3000) {
                        stop();
                        Logger.d(TAG + "_MEDIAPLAYER", "restart the same song");
                        try {
                            playmp3(getCurrentlyPlayingSong());
                        } catch (Exception e) {
                            Logger.e(TAG + "_MEDIAPLAYER", "error while getting previous song 1" + e);
                        }
                    } else {
                        stop();
                        try {
                            Logger.d(TAG + "_MEDIAPLAYER", "prev");
                            playmp3(MusicCollectionManager.getInstance().getPreviousSong());
                        } catch (Exception e) {
                            Logger.e(TAG + "_MEDIAPLAYER", "error while getting previous song 2" + e);
                        }
                    }
                }
            }
        }
    };

    private void requestAudioFocus() {
        am = (AudioManager) TrashPlayService.getContext().getSystemService(Context.AUDIO_SERVICE);

        int result = am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            selectSongAndPlay();
        }
    }


    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        public void onAudioFocusChange(int focusChange) {
            if (mp != null) {
                AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    ducking(audio);
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    unDucking(audio);

                }
            }
        }
    };

    private void unDucking(AudioManager audio) {
        Logger.d(TAG + "_MEDIAPLAYER", "Current" + currentVolume);
        Logger.d(TAG + "_MEDIAPLAYER", "Audiofocus GAIN");
        float max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float p = currentVolume / (max / 100);
        p = p / 100;
        mp.setVolume(p, p);
    }

    private void ducking(AudioManager audio) {
        currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        Logger.d(TAG + "_MEDIAPLAYER", "Current" + currentVolume);
        Logger.d(TAG + "_MEDIAPLAYER", "Audiofocus LOST");
        mp.setVolume(0.1f, 0.1f);
    }

    private void selectSongAndPlay() {
        Song nextSong = null;
        try {
            nextSong = MusicCollectionManager.getInstance().getNextSong();
        } catch (Exception e) {
            Logger.e(TAG + "_MEDIAPLAYER", "Error while start song" + e);
        }
        playmp3(nextSong);
    }

    public static int playPosition() {
        if (mp != null) {
            return mp.getCurrentPosition();
        }
        return 0;
    }

    public static int playLength() {
        int p = 0;
        if (mp != null) {
            try {
                p = getCurrentlyPlayingSong().getDuration();
                int p2 = mp.getDuration();
                if (p == 0 || p2!=p) {
                    p = mp.getDuration();
                    SongHelper.setDuration(getCurrentlyPlayingSong(), p);
                }
            } catch (Exception e) {
                return 0;
            }
        }
        return p;
    }

    public static String playPositionAsTimeString() {
        String result = "";
        if (mp != null) {
            try {
                int p = mp.getCurrentPosition();
                result = TrashPlayUtils.getStringFromIntInMilliSeconds(p);
            } catch (Exception e) {
                if (TrashPlayService.serviceRunning()) {
                    TrashPlayService.getContext().toast("Error1");
                }
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String playLengthAsTimeString() {
        String result = "";
        if (mp != null) {
            try {
                int p = getCurrentlyPlayingSong().getDuration();
                if (p == 0l) {
                    try {
                        p = mp.getDuration();
                        SongHelper.setDuration(getCurrentlyPlayingSong(), p);
                    } catch (Exception e) {
                        Logger.d(TAG + "_MEDIAPLAYER", "stupid length exceptione");
                        p = 180;
                    }
                }
                result = TrashPlayUtils.getStringFromIntInMilliSeconds(p);
            } catch (Exception e) {
                if (TrashPlayService.serviceRunning()) {
                    TrashPlayService.getContext().toast("Error2");
                }
                e.printStackTrace();
            }
        }
        return result;
    }

    public static Song getCurrentlyPlayingSong() {
        return currentlyPlayingSong;
    }

    private void setCurrentlyPlayingSong(Song song) {
        currentlyPlayingSong = song;
    }

    public static boolean isPlaying() {
        return currentlyPlayingSong!=null;
    }

    private void registerIntentFilters() {
        IntentFilter inf = new IntentFilter(ACTION_START_MUSIC);
        IntentFilter inf2 = new IntentFilter(ACTION_STOP_MUSIC);
        IntentFilter inf3 = new IntentFilter(ACTION_NEXT_SONG);
        IntentFilter inf4 = new IntentFilter(ACTION_PREV_SONG);
        IntentFilter inf5 = new IntentFilter(ACTION_PAUSE_SONG);
        registerReceiver(mReceiver, inf);
        registerReceiver(mReceiver, inf2);
        registerReceiver(mReceiver, inf3);
        registerReceiver(mReceiver, inf4);
        registerReceiver(mReceiver, inf5);
    }
}

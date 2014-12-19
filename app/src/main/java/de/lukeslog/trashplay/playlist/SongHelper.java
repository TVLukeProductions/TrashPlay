package de.lukeslog.trashplay.playlist;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.player.MusicPlayer;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.statistics.StatisticsCollection;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.TrashPlayUtils;

public class SongHelper {

    public static final String TAG = TrashPlayConstants.TAG;

    private static int numberOfViableSongs = 0;

    private MediaScannerConnection mConnection;
    private String mPath;
    private String mMimeType;

    public static boolean localFileExists(Song song) {
        return StorageManager.doesFileExists(song);
    }

    public static void addSongToPlayList(Song song, PlayList playList) {
        if(song.getPlayLists()!=null) {
            String playlistString = PlayListHelper.getPlayListEncodingString(playList);
            if (song.getPlayLists().contains(playlistString)) {
                song.setPlayLists(song.getPlayLists().replace(playlistString, ""));
            }
            if (!song.getPlayLists().contains(playlistString)) {
                song.setPlayLists(song.getPlayLists() + PlayListHelper.getPlayListEncodingString(playList) + " ");
                song.save();
            }
        }
        else
        {
            song.setPlayLists(PlayListHelper.getPlayListEncodingString(playList) + " ");
        }
    }

    public static void refreshLastUpdate(String songFileName) {
        Song song = getSongByFileName(songFileName);
        DateTime now = new DateTime();
        song.setLastUpdate(now.getMillis());
        song.save();
    }

    public static void refreshLastUpdate(Song song) {
        DateTime now = new DateTime();
        song.setLastUpdate(now.getMillis());
        song.save();
    }

    public static void removeFromPlayList(Song song, PlayList playList) {
        song.getPlayLists().replace("", PlayListHelper.getPlayListEncodingString(playList));
        if(song.getPlayLists().isEmpty()) {
            song.setToBeDeleted(true);
        }
        song.save();
    }

    public static void getMetaData(Song song)
    {

        File file = new File(StorageManager.LOCAL_STORAGE+song.getFileName());
        String[] metadata = new String[2];
        //first, get stuff from the filename

        metadata[0]=file.getName();
        metadata[1]=" ";
        Logger.d(TAG, "olol");
        try {
            Mp3File mp3file = new Mp3File(file.getAbsolutePath());
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                Logger.d(TAG, "Title: " + id3v2Tag.getTitle());
                Logger.d(TAG, "===Artist: " + id3v2Tag.getArtist());
                song.setArtist(id3v2Tag.getArtist());
                song.setSongName(id3v2Tag.getTitle());
                song.save();
                return;
            } else if (mp3file.hasId3v1Tag()) {
                ID3v1 id3v1Tag = mp3file.getId3v1Tag();
                Logger.d(TAG, "Title: " + id3v1Tag.getTitle());
                Logger.d(TAG, "---Artist: " + id3v1Tag.getArtist());
                song.setArtist(id3v1Tag.getArtist());
                song.setSongName(id3v1Tag.getTitle());
                song.save();
                return;
            } else {
                metadata = getSongNameFromFile(file);
            }
        } catch(Exception e) {
            metadata = getSongNameFromFile(file);
        }
        //This is unsafe insofar as that songs or artists names might actually have on a vowel + "?"
        // but that's less likely than it being a
        //false interpretation of the good damn id3 lib given that (at least right now) lots of
        // German stuff is in the
        //trashplaylist

        //also, if the id3 lib works well, this code is never executed...
        for(int i=0; i<metadata.length; i++)
        {
            metadata[i]=metadata[i].replace("A?", "Ä");
            metadata[i]=metadata[i].replace("a?", "ä");
            metadata[i]=metadata[i].replace("U?", "Ü");
            metadata[i]=metadata[i].replace("u?", "ü");
            metadata[i]=metadata[i].replace("O?", "Ö");
            metadata[i]=metadata[i].replace("o?", "ö");
        }
        song.setArtist(metadata[0]);
        song.setSongName(metadata[1]);
        song.save();
    }

    private static String[] getSongNameFromFile(File file) {
        String fn = file.getName();
        String[] metadata = new String[2];
        if (fn.contains("-") && fn.endsWith("mp3")) {
            fn = fn.replace(".mp3", "");
            String[] spl = fn.split("-");
            if (spl.length == 2) {
                metadata[0] = spl[0];
                metadata[0] = metadata[0].trim();
                metadata[1] = spl[1];
                metadata[1] = metadata[1].trim();
                Logger.d(TAG, "----------->ARTIST():" + spl[0]);
                Logger.d(TAG, "----------->SONG():" + spl[1]);
                metadata[1] = spl[1];
                metadata[0] = spl[0];
            }
        }
        return metadata;
    }

    public static String getTitleInfoAsString(Song song) {
        Logger.d(TAG, "getTitleInfoAsString(Song)");
        if(song!=null) {
            Logger.d(TAG, "not null"+song.getFileName());
            try {
                if (song.getSongName().equals("") && song.getArtist().equals("")) {
                    SongHelper.getMetaData(song);
                }
                if (!song.getSongName().equals("") && !song.getArtist().equals("")) {
                    return song.getArtist() + " - " + song.getSongName();
                } else {
                    return song.getFileName();
                }
            } catch (Exception e) {
                Logger.e(TAG, "exception cought...");
              return null;
            }
        }
        Logger.e(TAG, "NULL");
        return null;
    }

    public static String getTitleInfoAsStringWithPlayCount(Song song) {
        String songName = SongHelper.getTitleInfoAsString(song);
        if(songName!=null) {
            songName = songName + " (" + song.getPlays() + ")";
        }
        return songName;
    }

    public static boolean isInPlayList(Song song, PlayList playList) {
        String playlists = song.getPlayLists();
        if(playlists.contains(PlayListHelper.getPlayListEncodingString(playList))) {
            return true;
        }
        return false;
    }

    public static Song getSongByFileName(String fileName) {
        Song resultSong = null;
        resultSong = new Select().from(Song.class).where("fileName = ?", fileName).executeSingle();
        return resultSong;
    }

    public static void determineNumberOfViableSongs() {
        SongHelper.removeSongsThatAreToBeDeleted();
        List<Song> songs = SongHelper.getAllSongs();
        int n = 0;
        for (Song s : songs) {
            if (!s.isToBeDeleted() && !s.isToBeUpdated() && s.isInActiveUse()) {
                n++;
            }
        }
        numberOfViableSongs = n;
    }


    public static int getNumberOfViableSongs() {
        if(numberOfViableSongs==0){
            determineNumberOfViableSongs();
        }
        return numberOfViableSongs;
    }

    static void removeSongsThatAreToBeDeleted() {
        Logger.d(TAG, "removeSongsThatAreToBeDeleted()");
        if (TrashPlayService.serviceRunning()) {
            Logger.d(TAG, "service is running()");
            try {

                List<Song> result = new Select().from(Song.class).where("toBeDeleted = ?", "1").execute();

                boolean currentlyPlayingSongIsToBeDeleted = false;
                for (Song theSong : result) {
                    if (theSong.isToBeDeleted() && !theSong.equals(MusicPlayer.getCurrentlyPlayingSong())) {
                        StorageManager.deleteSongFromLocalStorage(theSong);
                    }
                }
                if (!currentlyPlayingSongIsToBeDeleted) {
                    new Delete().from(Song.class).where("toBeDeleted = ?", "1").execute();
                }
            } catch (Exception e) {
                    Logger.e(TAG, "Invalid Databse. Delete everything 2");
            }
        } else {
            Logger.d(TAG, "service is NOT running");
        }
    }

    static List<Song> getAllSongs() {
        Logger.d(TAG, "get all songs");
        List<Song> songs = new ArrayList<Song>();
        try {
            if (TrashPlayService.serviceRunning()) {
                songs = new Select().from(Song.class).execute();
            }
        } catch (Exception e) {

            Log.e(TAG, "Invalid Databse. Delete everything 1");
        }
        return songs;
    }

    public static List<Song> getAllSongsOrderedByPlays() {
        Logger.d(TAG, "get all songs");
        List<Song> songs = new ArrayList<Song>();
        try {
            if (TrashPlayService.serviceRunning()) {
                songs = new Select().from(Song.class).orderBy("plays DESC").execute();
            }
        } catch (Exception e) {

            Log.e(TAG, "Invalid Databse. Delete everything 1");
        }
        return songs;
    }

    public static void resetPlayList(Song song) {
        song.setPlayLists("");
    }

    public static Song createSong(String localFileName, PlayList playList) {
        if(playList!=null) {
            Logger.d(TAG, "lets first find out if we know this song");
            Song s = getSongByFileName(localFileName);
            if (s != null) {
                addSongToPlayList(s, playList);
                return s;
            } else {
                Logger.d(TAG, "create Song");
                Song newSong = new Song();
                Logger.d(TAG, "1");
                newSong.setFileName(localFileName);
                Logger.d(TAG, "2");
                getMetaData(newSong);
                Logger.d(TAG, "3");
                addSongToPlayList(newSong, playList);
                Logger.d(TAG, "4");
                refreshLastUpdate(newSong);
                newSong.setInActiveUse(true);
                newSong.save();
                try {
                    metaDataUpdate(newSong);
                } catch(Exception e) {
                    Logger.e(TAG, "error while tring to fix metadata");
                }
                return newSong;
            }
        }
        return null;
    }

    static boolean isSongInCollection(String fileName) {
        List<Song> allSongs = SongHelper.getAllSongs();
        for (Song song : allSongs) {
            if (song.getFileName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    public static void finishedWithSong(String songName){
        Song song = getSongByFileName(songName);
        song.setPlays(song.getPlays() + 1);
        long lastPlayed = song.getLastPlayed();
        DateTime now = new DateTime();
        long nowInMillis = now.getMillis();
        if(lastPlayed==0){
            lastPlayed=nowInMillis;
        }
        song.setLastPlayed(nowInMillis);
        if(TrashPlayService.getContext().isInTrashMode()) {
            StatisticsCollection.finishedSong(songName, nowInMillis - lastPlayed);
        }
        song.save();
        //Generate other global statistics...
    }

    public static List<PlayList> getAllPlayListsFromSong(Song song) {

        List<PlayList> result = new ArrayList<PlayList>();

        try {
            String pls = song.getPlayLists();
            String[] playListStrings = pls.split(",");
            for (String playlistString : playListStrings) {
                playlistString = playlistString.trim();
                if (playlistString.length() > 5) { //O.M.G
                    String[] parts = playlistString.split("/");
                    if(parts.length==2) {
                        String remotestorage = parts[0];
                        String remotepath = parts[1];
                        remotepath = remotepath.replace(",", "");
                        remotestorage = remotestorage.replace(",", "");
                        remotepath = remotepath.trim();
                        remotestorage = remotestorage.trim();
                        List<PlayList> playlists = PlayListHelper.getAllPlayLists();
                        for (PlayList playlist : playlists) {
                            if (playlist.getRemotePath().equals(remotepath) && playlist.getRemoteStorage().equals(remotestorage)) {
                                result.add(playlist);
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            Logger.e(TAG, "exception when retrieving palylists");
        }
        return result;
    }

    public static void setDuration(Song song, int p) {
        Song theSong = getSongByFileName(song.getFileName());
        Logger.d(TAG, "SET DURATION!");
        theSong.setDuration(p);
        theSong.save();
    }

    public static void setActive(Song song, boolean active) {
        Song theSong = getSongByFileName(song.getFileName());
        theSong.setInActiveUse(active);
        theSong.save();
    }

    public static void metaDataUpdate(final Song song) throws IOException {
        Logger.d(TAG, "METADATA UPDATE");
        if(song.getDuration()==0){
            File mediafile = new File(StorageManager.LOCAL_STORAGE + song.getFileName());
            String musicPath = mediafile.getAbsolutePath();
            final MediaPlayer mplocal = new MediaPlayer();
            mplocal.setDataSource(musicPath);
            mplocal.prepareAsync();
            mplocal.setOnPreparedListener( new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    Logger.d(TAG, "on Prepared for "+song.getFileName());
                    int duration = mediaPlayer.getDuration();
                    Logger.d(TAG, "->"+duration);
                    Logger.d(TAG, "->"+ TrashPlayUtils.getStringFromIntInMilliSeconds(duration));
                    song.setDuration(duration);
                    song.save();
                }
            });
        }
        if(song.getSongName().equals("") || song.getArtist().equals("")) {
            getMetaData(song);
        }
    }

    private static class MediaScannerNotifier implements
            MediaScannerConnection.MediaScannerConnectionClient {
        private Context mContext;
        private MediaScannerConnection mConnection;
        private String mPath;
        private String mMimeType;

        public MediaScannerNotifier(Context context, String path, String mimeType) {
            mContext = context;
            mPath = path;
            mMimeType = mimeType;
            mConnection = new MediaScannerConnection(context, this);
            mConnection.connect();
        }

        public void onMediaScannerConnected() {
            Logger.d(TAG, "MEDIASCANNER CONNECTED!");
            mConnection.scanFile(mPath, mMimeType);
        }

        public void onScanCompleted(String path, Uri uri) {
            Logger.d(TAG, "MEDIASCANNER COMPLETED");
            MediaPlayer mplocal = new MediaPlayer();
            try {
                mplocal.setDataSource(path);
                int duration = mplocal.getDuration();
                Logger.d(TAG, path+" DURATION"+duration);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

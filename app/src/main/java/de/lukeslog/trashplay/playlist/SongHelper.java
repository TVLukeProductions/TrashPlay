package de.lukeslog.trashplay.playlist;

import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.player.MusicPlayer;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;

public class SongHelper {

    public static final String TAG = TrashPlayConstants.TAG;

    private static int numberOfViableSongs = 0;

    public static boolean localFileExists(Song song) {
        return StorageManager.doesFileExists(song);
    }

    public static void addSongToPlayList(Song song, PlayList playList) {
        Logger.d(TAG, "Add Song To PlayList "+playList.getRemotePath());
        Logger.d(TAG, "Current Playlist encoding: "+song.getPlayLists());
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
        Logger.d(TAG, "NOW: "+song.getPlayLists());
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

        String tempsong="";
        String tempartist ="";
        String fn = file.getName();
        if(fn.contains("-") && fn.endsWith("mp3"))
        {
            fn=fn.replace(".mp3", "");
            String[] spl = fn.split("-");
            if(spl.length==2)
            {
                metadata[0]=spl[0];
                metadata[0]=metadata[0].trim();
                metadata[1]=spl[1];
                metadata[1]=metadata[1].trim();
                Logger.d(TAG, "----------->ARTIST():"+spl[0]);
                Logger.d(TAG, "----------->SONG():"+spl[1]);
                tempsong=metadata[1];
                tempartist = metadata[0];
            }
        }
         //try to get the ID3 Tag... but this is mostly shit...
        try
        {
            //Logger.d(TAG, "md1");
            MP3File mp3 = new MP3File(file);

            //Logger.d(TAG, "md2");
            ID3v1 id3 = mp3.getID3v1Tag();
            //Logger.d(TAG, "md3");
            //Logger.d(TAG, "md3d");
            metadata[0] = id3.getArtist();
            //Logger.d(TAG, "md4");
            //Logger.d(TAG, "----------->ARTIST:"+metadata[0]);
            metadata[1] = id3.getSongTitle();
            //Logger.d(TAG, "md5");
            //Logger.d(TAG, "----------->SONG:"+metadata[1]);
            //Logger.d(TAG, "md6");
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
            metadata[0]=file.getName();
            metadata[1]=" ";
            //Logger.d(TAG, "----------->ARTIST():"+metadata[0]);
            //Logger.d(TAG, "----------->SONG():"+metadata[1]);
        }
        catch (TagException e1)
        {
            e1.printStackTrace();
            metadata[0]=file.getName();
            metadata[1]=" ";
            //Logger.d(TAG, "----------->ARTIST():"+metadata[0]);
            //Logger.d(TAG, "----------->SONG():"+metadata[1]);
        }
        catch(Exception ex)
        {
            Logger.e(TAG, "There has been an exception while extracting ID3 Tag Information from the MP3");
            fn = file.getName();
            if(fn.contains("-") && fn.endsWith("mp3"))
            {
                metadata[0]=tempartist;
                metadata[1]=tempsong;
            }
            else
            {
                metadata[0]=file.getName();
                metadata[1]=" ";
                Logger.d(TAG, "----------->ARTIST():"+metadata[0]);
                Logger.d(TAG, "----------->SONG():"+metadata[1]);
            }
        }
        if(metadata[0].length()<tempartist.length() || metadata[1].length()<tempsong.length()) {
            metadata[0]=tempartist;
            metadata[1]=tempsong;
        }
        //This is unsafe insofar as that songs or artists names might actually have on a vowel + "?" but thats less likely than it being a
        //false interpretation of the good damn id3 lib given that (at least right now) lots of German stuff is in the
        //trashplaylist
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
        Logger.d(TAG, "===================================================");
        SongHelper.removeSongsThatAreToBeDeleted();
        List<Song> songs = SongHelper.getAllSongs();
        int n = 0;
        for (Song s : songs) {
            Logger.d(TAG, s.getFileName());
            Logger.d(TAG, ""+s.isInActiveUse());
            if (!s.isToBeDeleted() && !s.isToBeUpdated() && s.isInActiveUse()) {
                n++;
            }
        }
        numberOfViableSongs = n;
        Logger.d(TAG, "===================================================");
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
        song.setLastPlayed(nowInMillis);
        //TODO: change the overall playcount
        //TODO: get the average time between plays
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
                Logger.d(TAG, "playListString: "+playlistString);
                if (playlistString.length() > 5) { //O.M.G
                    String[] parts = playlistString.split("/");
                    if(parts.length==2) {
                        String remotestorage = parts[0];
                        String remotepath = parts[1];
                        remotepath = remotepath.replace(",", "");
                        remotestorage = remotestorage.replace(",", "");
                        remotepath = remotepath.trim();
                        remotestorage = remotestorage.trim();
                        Logger.d(TAG, "remotestorage=" + remotestorage);
                        Logger.d(TAG, "remotepath=" + remotepath);
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
        theSong.setDurationInSeconds(p);
        theSong.save();
    }

    public static void setActive(Song song, boolean active) {
        Song theSong = getSongByFileName(song.getFileName());
        theSong.setInActiveUse(active);
        theSong.save();
    }
}

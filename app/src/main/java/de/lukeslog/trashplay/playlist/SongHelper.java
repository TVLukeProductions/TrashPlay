package de.lukeslog.trashplay.playlist;

import android.util.Log;

import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class SongHelper {

    public static final String TAG = TrashPlayConstants.TAG;

    public static boolean localFileExists(Song song) {
        return StorageManager.doesFileExists(song);
    }

    public static void addSongToPlayList(Song song, PlayList playList) {
        Log.d(TAG, "Add Song To PlayList "+playList.getRemotePath());
        song.getPlayLists().add(playList);
    }

    public static void refreshLastUpdate(String songFileName) {
        Song song = getSongByFileName(songFileName);
        DateTime now = new DateTime();
        song.setLastUpdate(now.getMillis());
    }

    public static void refreshLastUpdate(Song song) {
        DateTime now = new DateTime();
        song.setLastUpdate(now.getMillis());
    }

    public static void removeFromPlayList(Song song, PlayList playList) {
        song.getPlayLists().remove(playList);
        if(song.getPlayLists().isEmpty()) {
            song.setToBeDeleted(true);
        }
    }

    public static void resetPlayList(Song song) {
        RealmList<PlayList> x = song.getPlayLists();
        x.clear();
        song.setPlayLists(x);
    }

    public static void getMetaData(Song song)
    {
        File file = new File(StorageManager.LOCAL_STORAGE+song.getFileName());
        String[] metadata = new String[2];
        metadata[0]=file.getName();
        metadata[1]=" ";
        try
        {
            //Log.d(TAG, "md1");
            MP3File mp3 = new MP3File(file);

            //Log.d(TAG, "md2");
            ID3v1 id3 = mp3.getID3v1Tag();
            //Log.d(TAG, "md3");
            //Log.d(TAG, "md3d");
            metadata[0] = id3.getArtist();
            //Log.d(TAG, "md4");
            //Log.d(TAG, "----------->ARTIST:"+metadata[0]);
            metadata[1] = id3.getSongTitle();
            //Log.d(TAG, "md5");
            //Log.d(TAG, "----------->SONG:"+metadata[1]);
            //Log.d(TAG, "md6");
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
            metadata[0]=file.getName();
            metadata[1]=" ";
            //Log.d(TAG, "----------->ARTIST():"+metadata[0]);
            //Log.d(TAG, "----------->SONG():"+metadata[1]);
        }
        catch (TagException e1)
        {
            e1.printStackTrace();
            metadata[0]=file.getName();
            metadata[1]=" ";
            //Log.d(TAG, "----------->ARTIST():"+metadata[0]);
            //Log.d(TAG, "----------->SONG():"+metadata[1]);
        }
        catch(Exception ex)
        {
            Logger.e(TAG, "There has been an exception while extracting ID3 Tag Information from the MP3");
            String fn = file.getName();
            fn.replace("�", "-"); //wired symbols that look alike
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
                }
            }
            else
            {
                metadata[0]=file.getName();
                metadata[1]=" ";
                Logger.d(TAG, "----------->ARTIST():"+metadata[0]);
                Logger.d(TAG, "----------->SONG():"+metadata[1]);
            }
        }
        //This is unsafe insofar as that songs or artists names might actually have on a vowel + "?" but thats less likely than it being a
        //false interpretation of the good damn id3 lib given that (at least right now) lots of German stuff is in the
        //trashplaylist
        for(int i=0; i<metadata.length; i++)
        {
            metadata[i]=metadata[i].replace("A?", "�");
            metadata[i]=metadata[i].replace("a?", "�");
            metadata[i]=metadata[i].replace("U?", "�");
            metadata[i]=metadata[i].replace("u?", "�");
            metadata[i]=metadata[i].replace("O?", "�");
            metadata[i]=metadata[i].replace("o?", "�");
        }
        song.setArtist(metadata[0]);
        song.setSongName(metadata[1]);
    }

    public static String getTitleInfoAsString(Song song) {
        Logger.d(TAG, "getTitleInfoASString()");
        if(song!=null) {
            try {
                Logger.d(TAG, song.getFileName());
                if (song.getSongName().equals("") && song.getArtist().equals("")) {
                    SongHelper.getMetaData(song);
                }
                if (!song.getSongName().equals("") && !song.getArtist().equals("")) {
                    return song.getArtist() + " - " + song.getSongName();
                } else {
                    return song.getFileName();
                }
            } catch (Exception e) {
              return null;
            }
        }
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
        return song.getPlayLists().contains(playList);
    }

    public static Song getSongByFileName(String fileName) {
        Song resultSong = null;
        Realm realm = Realm.getInstance(TrashPlayService.getContext());
        realm.beginTransaction();

        RealmQuery<Song> query = realm.where(Song.class).equalTo("fileName", fileName);

        RealmResults<Song> result = query.findAll();

        for (Song song : result) {
            resultSong = song;
            try {
                song.getFileName();
            } catch(Exception e) {
                Log.e(TAG, "No filename in SongHelper");
            }
        }
        realm.commitTransaction();
        return resultSong;
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
        //Generate other global statistics...
    }
}

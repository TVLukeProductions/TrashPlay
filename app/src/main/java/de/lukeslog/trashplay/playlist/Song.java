package de.lukeslog.trashplay.playlist;

import android.util.Log;

import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.lukeslog.trashplay.cloudstorage.CloudSynchronizationService;
import de.lukeslog.trashplay.cloudstorage.LocalStorage;
import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;

public class Song {

    private ArrayList<PlayList> playLists = new ArrayList<PlayList>();
    private String songName;
    private String artist;
    private String fileName;
    private DateTime lastUpdate;
    private int durationInSeconds=0;
    private int plays=0;

    private boolean toBeUpdated;
    private boolean toBeDeleted;

    public static final String TAG = TrashPlayConstants.TAG;

    Song(String fileName) {
        this.fileName=fileName;
        String[] metadata = getMetaData(new File(StorageManager.LOCAL_STORAGE+fileName));
        artist = metadata[0];
        songName = metadata[1];
    }

    public void refreshLastUpdate() {
        lastUpdate=new DateTime();
    }

    public static String[] getMetaData(File file)
    {
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
            Log.e(TAG, "There has been an exception while extracting ID3 Tag Information from the MP3");
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
                    Log.d(TAG, "----------->ARTIST():"+spl[0]);
                    Log.d(TAG, "----------->SONG():"+spl[1]);
                }
            }
            else
            {
                metadata[0]=file.getName();
                metadata[1]=" ";
                Log.d(TAG, "----------->ARTIST():"+metadata[0]);
                Log.d(TAG, "----------->SONG():"+metadata[1]);
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
        return metadata;
    }

    public void addToPlayList(PlayList playList) {
        Log.d(TAG, "Add Song To PLAylIst "+playList.getName());
        playLists.add(playList);
    }

    public String getFileName() {
        return fileName;
    }

    public DateTime getLastUpdate() {
        return lastUpdate;
    }

    public boolean isInPlayList(PlayList playList) {
        return playLists.contains(playList);
    }

    public boolean isToBeDeleted() {
        return toBeDeleted;
    }

    public void setToBeDeletedTrue() {
        setToBeDeleted(true);
        MusicCollectionManager.getInstance().determineNumberOfViableSongs();
    }

    public void setToBeDeletedFalse() {
        setToBeDeleted(false);
        MusicCollectionManager.getInstance().determineNumberOfViableSongs();
    }

    private void setToBeDeleted(boolean toBeDeleted) {
        this.toBeDeleted = toBeDeleted;
    }

    public boolean isToBeUpdated() {
        return toBeUpdated;
    }

    public void setToBeUpdated(boolean toBeUpdated) {
        this.toBeUpdated = toBeUpdated;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public void removeFromPlayList(PlayList playList) {
        playLists.remove(playList);
        if(playLists.isEmpty()) {
            setToBeDeleted(true);
        }
    }

    public void setDurationInSeconds(int durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public int getDurationInSeconds() {
        return durationInSeconds;
    }

    public String getTitleInfoAsString() {
        if(songName!=null && artist!=null) {
            return artist+" - "+songName;
        } else {
            return fileName;
        }
    }

    public String getTitleInfoAsStringWithPlayCount() {
        String songName = getTitleInfoAsString();
        songName= songName+" ("+getPlays()+")";
        return songName;
    }

    public boolean localFileExists() {

        Log.d(TAG, "LocalFileExists? "+StorageManager.LOCAL_STORAGE+getFileName());
        return StorageManager.doesFileExists(this);
    }

    public void resetPlayList() {
        playLists = new ArrayList<PlayList>();
    }

    public void finishedPlay() {
        plays++;
    }

    public int getPlays(){
        return plays;
    }
}

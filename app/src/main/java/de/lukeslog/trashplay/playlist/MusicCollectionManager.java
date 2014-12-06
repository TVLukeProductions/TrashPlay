package de.lukeslog.trashplay.playlist;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.cloudstorage.DropBox;
import de.lukeslog.trashplay.constants.TrashPlayConstants;

/**
 * This class is a singleton which is in charge of managing the access of the music player to the
 * music. No class handles PlayLists or Songs directly, everything goes through this class
 */
public class MusicCollectionManager {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    private static final int LENGTH_OF_PLAYLIST = 10;

    private static MusicCollectionManager instance = null;

    private HashMap<String, PlayList> playLists = new HashMap<String, PlayList>();
    private HashMap<String, Song> songs = new HashMap<String, Song>();
    private int numberOfViableSongs = 0;

    private ArrayList<Song> nextSongs = new ArrayList<Song>();

    private MusicCollectionManager() {

    }

    public static synchronized MusicCollectionManager getInstance() {
        if (instance == null) {
            instance = new MusicCollectionManager();
        }
        return instance;
    }

    public void finishedSong() {
        Log.d(TAG, "finished Song");
        if (!nextSongs.isEmpty()) {
            Log.d(TAG, "remove 0");
            Song s = nextSongs.remove(0);
            s.finishedPlay();
        }

    }

    public Song getNextSong() {
        Log.d(TAG, "get Next Song");
        refreshPlayList();
        if (!nextSongs.isEmpty()) {

            Song song = nextSongs.get(0);
            if (song != null) {
                Log.d(TAG, "next song: " + song.getTitleInfoAsString());
            } else {
                Log.d(TAG, "SONG IS NULL! THIS IS FUCKED UP");
            }
            return song;
        }
        return null;
    }

    public ArrayList<Song> getListOfNextSongs() {
        return nextSongs;
    }

    private void refreshPlayList() {
        //TODO if playList isEmpty check if one has to be recovered from the settings
        Log.d(TAG, "refresh playlist");
        for (int i = 0; i < nextSongs.size(); i++) {
            Log.d(TAG, "refresh");
            nextSongs.set(i, replaceSongForReasons(nextSongs.get(i)));
        }
        if (nextSongs.size() < LENGTH_OF_PLAYLIST) {
            int numberOfSongsToAdd = LENGTH_OF_PLAYLIST - nextSongs.size();
            Log.d(TAG, "refresh has to add " + numberOfSongsToAdd + " songs");
            for (int i = 0; i < numberOfSongsToAdd; i++) {
                Song s = pickASong();
                Log.d(TAG, "->" + i + " " + s.getTitleInfoAsString());
                nextSongs.add(s);
            }
        }
        Log.d(TAG, "done with playlist refreshing");
    }

    public Song pickASong() {
        Log.d(TAG, "pickASong()");
        SongSelector songSelector = SongSelectorFactory.getSongSelector(SongSelector.EQUAL_PLAY_SONG_SELECTOR);
        Song possibleSong = songSelector.getASong();
        Log.d(TAG, "its probably gona be " + possibleSong.getArtist() + " - " + possibleSong.getSongName());
        return replaceSongForReasons(possibleSong);
    }

    private Song replaceSongForReasons(Song possibleSong) {
        Log.d(TAG, "check if song needs to be replaced (" + possibleSong.getTitleInfoAsString() + ")");
        if (getNumberOfViableSongs() > 0) {
            Log.d(TAG, "number if viable songs is more than 0, thats good");
            if (possibleSong.isToBeDeleted()) {
                Log.d(TAG, "isToBeDeleted");
                return pickASong();
            }
            if (possibleSong.isToBeUpdated()) {
                Log.d(TAG, "isToBeUpdated");
                return pickASong();
            }
            if (!possibleSong.localFileExists()) {
                Log.d(TAG, "localFiledoesNotExist");
                return pickASong();
            }
            //TODO: check if the user wants to avoid repeating songs, replace song if its the not the next song but equals to the next song
            return possibleSong;
        }
        return null;
    }

    private PlayList createNewPlayList(StorageManager storageManager, String path) {
        PlayList playlist = new PlayList(storageManager, path);
        return playlist;
    }

    public void syncRemoteStorageWithDevice() throws Exception {
        new Thread(new Runnable() {
            public void run() {
                try {
                    findRemotePlayListFoldersAndCreateNewPlayLists();

                    synchronizePlayLists();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void synchronizePlayLists() throws Exception {
        Set<String> keys = playLists.keySet();
        for (String key : keys) {
            playLists.get(key).synchronizeFiles();
            determineNumberOfViableSongs();
        }
    }

    private void findRemotePlayListFoldersAndCreateNewPlayLists() throws Exception {
        if (DropBox.getInstance().isConnected()) {

            DropBox newDropBox = DropBox.getInstance();

            List<String> folderNames = newDropBox.getAllPlayListFolders();
            Log.d(TAG, "Done with the search");
            Log.d(TAG, "returned with a list of " + folderNames.size());
            for (String folderName : folderNames) {
                Log.d(TAG, folderName);
                if (!knownPlaylist(folderName)) {
                    playLists.put(folderName, createNewPlayList(newDropBox, folderName));
                }
            }
        }
    }

    private boolean knownPlaylist(String foldername) {
        return playLists.containsKey(foldername);
    }

    //TODO: Not sure I like the idea of this being static...
    public static List<String> getListOfAllowedFileEndings() {
        List<String> fileEndings = new ArrayList<String>();
        fileEndings.add("mp3");
        return fileEndings;
    }

    public boolean isSongInCollection(String fileName) {
        return songs.keySet().contains(fileName);
    }

    public void addToSongCollection(Song newSong) {
        Log.d(TAG, "new Song " + newSong.getFileName());
        songs.put(newSong.getFileName(), newSong);
        determineNumberOfViableSongs();
    }

    public Song getSongByFileName(String fileName) {
        return songs.get(fileName);
    }

    public List<Song> getSongsByPlayList(PlayList playList) {
        List<Song> songsInPlayList = new ArrayList<Song>();
        for (String songFileName : songs.keySet()) {
            Song theSong = songs.get(songFileName);
            if (theSong.isInPlayList(playList)) {
                songsInPlayList.add(theSong);
            }
        }
        return songsInPlayList;
    }

    public boolean collectionNotEmpty() {
        removeSongsThatAreToBeDeleted();
        return !(getNumberOfViableSongs()==0);
    }

    public void removeSongsThatAreToBeDeleted() {
        for (String songFileName : songs.keySet()) {
            Song theSong = songs.get(songFileName);
            if (theSong.isToBeDeleted()) {
                songs.remove(songFileName);
            }
        }
        determineNumberOfViableSongs();
    }

    public void determineNumberOfViableSongs() {
        int n = 0;
        for (Song s : songs.values()) {
            if (!s.isToBeDeleted() && !s.isToBeUpdated()) {
                n++;
            }
        }
        numberOfViableSongs = n;
    }

    public int getNumberOfViableSongs() {
        return numberOfViableSongs;
    }

    public HashMap<String, Song> getListOfSongs() {
        return songs;
    }
}

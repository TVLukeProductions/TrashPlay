package de.lukeslog.trashplay.playlist;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.cloudstorage.DropBox;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.player.MusicPlayer;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.service.TrashPlayServiceNotRunningException;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * This class is a singleton which is in charge of managing the access of the music player to the
 * music. No class handles PlayLists or Songs directly, everything goes through this class
 */
public class MusicCollectionManager {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    private static final int LENGTH_OF_PLAYLIST = 10;

    private static MusicCollectionManager instance = null;

    private int numberOfViableSongs = 0;
    private int numberOfActivatedPlayLists = 0;

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
            s.setPlays(s.getPlays() + 1);
        }

    }

    public Song getNextSong() {
        Log.d(TAG, "get Next Song");
        refreshPlayList();
        if (!nextSongs.isEmpty()) {

            Song song = nextSongs.get(0);
            if (song != null) {
                Log.d(TAG, "next song: " + SongHelper.getTitleInfoAsString(song));
            } else {
                Log.d(TAG, "SONG IS NULL! THIS IS FUCKED UP");
            }
            return song;
        }
        return null;
    }

    public ArrayList<Song> getListOfNextSongs() {
        if(TrashPlayService.serviceRunning()) {
            if (nextSongs.isEmpty()) {
                recoverNextSongsFromSettings();
            }
        }
        return nextSongs;
    }

    private void refreshPlayList() {
        if(nextSongs.isEmpty()) {
            recoverNextSongsFromSettings();
        }
        Log.d(TAG, "refresh playlist");
        for (int i = 0; i < nextSongs.size(); i++) {
            Log.d(TAG, "refresh");
            Song s = replaceSongForReasons(nextSongs.get(i));
            if(s!=null) {
                setNextSongPlayListInSettings(i, s.getFileName());
                nextSongs.set(i, s);
            }
        }
        nextSongs.remove(null);
        if (nextSongs.size() < LENGTH_OF_PLAYLIST) {
            int numberOfSongsToAdd = LENGTH_OF_PLAYLIST - nextSongs.size();
            Log.d(TAG, "refresh has to add " + numberOfSongsToAdd + " songs");
            for (int i = 0; i < numberOfSongsToAdd; i++) {
                Song song = pickASong();
                Log.d(TAG, "->" + i + " " + SongHelper.getTitleInfoAsString(song));
                setNextSongPlayListInSettings(nextSongs.size(), song.getFileName());
                nextSongs.add(song);
            }
        }
        Log.d(TAG, "done with playlist refreshing");
    }

    private void setNextSongPlayListInSettings(int i, String fileName) {
        Log.d(TAG, "put into settings");
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        SharedPreferences.Editor edit = settings.edit();
        edit.putString("nextSong_"+i, fileName);
        edit.commit();
    }

    private void recoverNextSongsFromSettings() {
        Log.d(TAG, "try to get the next plays from the settings");
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        if(settings!=null) {
            Log.d(TAG, "settings not null");
            for(int i=0; i<10; i++){
                String nextSongFileName = settings.getString("nextSong_"+i, "");
                Log.d(TAG, nextSongFileName);
                if(nextSongFileName.equals("")) {
                    break;
                }
                Log.d(TAG, "olol->");
                nextSongs.add(getSongByFileName(nextSongFileName));
            }
        }

    }

    public Song pickASong() {
        Log.d(TAG, "pickASong()");
        SongSelector songSelector = SongSelectorFactory.getSongSelector(SongSelector.EQUAL_PLAY_SONG_SELECTOR);
        Song possibleSong = songSelector.getASong();
        return replaceSongForReasons(possibleSong);
    }

    private Song replaceSongForReasons(Song possibleSong) {
        Log.d(TAG, "replaceForReasons...");
        Log.d(TAG, "check if song needs to be replaced (" + SongHelper.getTitleInfoAsString(possibleSong) + ")");
        if (getNumberOfViableSongs() > 0 && getNumberOfActivePlayLists() > 0) {
            Log.d(TAG, "number if viable songs is more than 0, thats good");
            if(possibleSong==null) {
                determineNumberOfViableSongs();
                return pickASong();
            }
            if (possibleSong.isToBeDeleted()) {
                Log.d(TAG, "isToBeDeleted");
                return pickASong();
            }
            if (possibleSong.isToBeUpdated()) {
                Log.d(TAG, "isToBeUpdated");
                return pickASong();
            }
            if (!SongHelper.localFileExists(possibleSong)) {
                Log.d(TAG, "localFiledoesNotExist");
                return pickASong();
            }
            if (!possibleSong.isInActiveUse()) {
                return possibleSong;
            }
            return possibleSong;
            //TODO: check if the user wants to avoid repeating songs, replace song if its the not the next song but equals to the next song
        }
        return null;
    }

    private PlayList createNewPlayList(StorageManager storageManager, String path) throws TrashPlayServiceNotRunningException {
        if (TrashPlayService.serviceRunning()) {
            Realm realm = Realm.getInstance(TrashPlayService.getContext());
            realm.beginTransaction();
            PlayList playlist = realm.createObject(PlayList.class);
            playlist.setRemoteStorage(storageManager.getStorageType());
            playlist.setRemotePath(path);
            realm.commitTransaction();
            return playlist;
        }
        throw new TrashPlayServiceNotRunningException();
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
        Log.d(TAG, "Synchronize");
        ArrayList<PlayList> allPlayLists = getAllPlayLists();
        Log.d(TAG, "getAll");
        for (PlayList playlist : allPlayLists) {
            PlayListFileSynchronizer.synchronize(playlist);
            determineNumberOfViableSongs();
        }
        Log.d(TAG, "ok.");
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
                    createNewPlayList(newDropBox, folderName);
                }
            }
        }
    }

    private boolean knownPlaylist(String foldername) {

        ArrayList<PlayList> allPlayLists = getAllPlayLists();
        for(PlayList playList : allPlayLists) {
            if(playList.getRemotePath().equals(foldername)) {
               return true;
            }
        }
        return false;
    }

    //TODO: Not sure I like the idea of this being static...
    public static List<String> getListOfAllowedFileEndings() {
        List<String> fileEndings = new ArrayList<String>();
        fileEndings.add("mp3");
        return fileEndings;
    }

    public boolean isSongInCollection(String fileName) {
        ArrayList<Song> allSongs = getAllSongs();
        for(Song song : allSongs) {
            if(song.getFileName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    public Song getSongByFileName(String fileName) {
        ArrayList<Song> allSongs = getAllSongs();
        for(Song song : allSongs) {
            if(song.getFileName().equals(fileName)) {
                return song;
            }
        }
        return null;
    }

    public List<Song> getSongsByPlayList(PlayList playList) {
        List<Song> songsInPlayList = new ArrayList<Song>();
        List<Song> allSongs = getAllSongs();
        for (Song song : allSongs) {
            if (SongHelper.isInPlayList(song, playList)) {
                songsInPlayList.add(song);
            }
        }
        return songsInPlayList;
    }

    public boolean collectionNotEmpty() {
        removeSongsThatAreToBeDeleted();
        return !(getNumberOfViableSongs() == 0);
    }

    public void removeSongsThatAreToBeDeleted() {
        if(TrashPlayService.serviceRunning()) {
            Realm realm = Realm.getInstance(TrashPlayService.getContext());
            realm.beginTransaction();

            RealmQuery<Song> query = realm.where(Song.class).equalTo("toBeDeleted", true);

            RealmResults<Song> result = query.findAll();

            boolean currentlyPlayingSongIsToBeDeleted=false;
            for (Song theSong : result) {
                if (theSong.isToBeDeleted() && !theSong.equals(MusicPlayer.getCurrentlyPlayingSong())) {
                    StorageManager.deleteSongFromLocalStorage(theSong);
                }
            }
            if(!currentlyPlayingSongIsToBeDeleted) {
                result.clear();
            }

            realm.commitTransaction();
            determineNumberOfViableSongs();
        }
    }

    public void determineNumberOfViableSongs() {
        ArrayList<Song> songs = getAllSongs();
        int n=0;
        for (Song s : songs) {
            if (!s.isToBeDeleted() && !s.isToBeUpdated() && s.isInActiveUse()) {
                n++;
            }
        }
        numberOfViableSongs = n;
    }

    public int getNumberOfViableSongs() {
        return numberOfViableSongs;
    }

    public ArrayList<PlayList> getAllPlayLists() {
        Log.d(TAG, "getAllPalyLists");
        ArrayList<PlayList> playlists = new ArrayList<PlayList>();
        if(TrashPlayService.serviceRunning()) {
            Log.d(TAG, "service running");
            Realm realm = Realm.getInstance(TrashPlayService.getContext());
            Log.d(TAG, "a");
            realm.beginTransaction();
            Log.d(TAG, "b");
            RealmQuery<PlayList> query = realm.where(PlayList.class);
            Log.d(TAG, "c");
            RealmResults<PlayList> result = query.findAll();
            Log.d(TAG, "x");
            for (PlayList playlist : result) {
                playlists.add(playlist);
            }
            Log.d(TAG, "y");
            realm.commitTransaction();
        }
        Log.d(TAG, "olol");
        return playlists;
    }

    public ArrayList<Song> getAllSongs() {
        ArrayList<Song> songs = new ArrayList<Song>();
        if(TrashPlayService.serviceRunning()) {
            Realm realm = Realm.getInstance(TrashPlayService.getContext());
            realm.beginTransaction();

            RealmQuery<Song> query = realm.where(Song.class);

            RealmResults<Song> result = query.findAll();

            for (Song song : result) {
                songs.add(song);
            }

            realm.commitTransaction();
        }
        return songs;
    }

    public ArrayList<Song> getListOfSongs() {
        return getAllSongs();
    }

    public int getNumberOfActivePlayLists() {
        return numberOfActivatedPlayLists;
    }

    public void determineNumberOfActivatedPlayLists() {
        Log.d(TAG, "getNumberOFActivatedPlayLIsts");
        int n= 0;
        ArrayList<PlayList> allplayLists = getAllPlayLists();
        for(PlayList playList : allplayLists) {
            if(playList.isActivated()) {
                n++;
            }
        }
        Log.d(TAG, "->"+n);
        numberOfActivatedPlayLists = n;
    }
}

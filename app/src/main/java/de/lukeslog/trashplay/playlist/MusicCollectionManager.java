package de.lukeslog.trashplay.playlist;

import android.content.SharedPreferences;
import android.util.Log;

import com.dropbox.client2.exception.DropboxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.lukeslog.trashplay.cloudstorage.CloudStorage;
import de.lukeslog.trashplay.cloudstorage.DropBox;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.umass.lastfm.Playlist;

/**
 * This class is a singleton which is in charge of managing the access of the music player to the
 * music. No class handles PlayLists or Songs directly, everything goes through this class
 */
public class MusicCollectionManager {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    private static MusicCollectionManager instance = null;

    private HashMap<String, PlayList> playLists = new HashMap<String, PlayList>();
    private HashMap<String, Song> songs = new HashMap<String, Song>();

    private MusicCollectionManager() {

    }

    public static synchronized MusicCollectionManager getInstance() {
        if (instance == null) {
            instance = new MusicCollectionManager();
        }
        return instance;
    }

    public Song getNextSong() {
        int randomsongnumber = (int) (Math.random() * (songs.size()));
        Song[] thesongs = songs.values().toArray(new Song[songs.size()]);
        return thesongs[randomsongnumber];
    }

    private PlayList createNewPlayList(CloudStorage cloudStorage, String path) {
        PlayList playlist = new PlayList(cloudStorage, path);
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
        }
    }

    private void findRemotePlayListFoldersAndCreateNewPlayLists() throws Exception {
        if (DropBox.getInstance().isConnected()) {

            DropBox newDropBox = DropBox.getInstance();

            List<String> folderNames = newDropBox.getAllPlayListFolders();
            Log.d(TAG, "Done with the search");
            Log.d(TAG, "returned with a list of " + folderNames.size());
            for (String foldername : folderNames) {
                Log.d(TAG, foldername);
                if (!knownPlaylist(foldername)) {
                    playLists.put(foldername, createNewPlayList(newDropBox, foldername));
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
        Log.d(TAG, "new Song "+newSong.getFileName());
        songs.put(newSong.getFileName(), newSong);
    }

    public Song getSongByFileName(String fileName) {
        return songs.get(fileName);
    }
}

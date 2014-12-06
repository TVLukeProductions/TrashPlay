package de.lukeslog.trashplay.playlist;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;

/**
 * PlayList implements a PlayList which is a music folder in a cloud storage
 */
public class PlayList {

    StorageManager remoteStorage;
    String remotePath;
    boolean activated=false;

    public static final String TAG = TrashPlayConstants.TAG;

    PlayList(StorageManager remoteStorage, String remotePath) {
        Log.d(TAG, "new PlayList "+remotePath);
        this.remoteStorage=remoteStorage;
        this.remotePath=remotePath;
    }

    void synchronizeFiles() throws Exception {
        Log.d(TAG, "Synchronize Files from PlayList");
        ArrayList<String> listOfFileNames = remoteStorage.
                getFileNameListWithEndings(MusicCollectionManager.getListOfAllowedFileEndings(), remotePath);
        Log.d(TAG, "PlayList has gotten a list of "+listOfFileNames.size()+" names");
        for(String fileName : listOfFileNames) {
            Log.d(TAG, "-> "+fileName);
            boolean inCollection = MusicCollectionManager.getInstance().isSongInCollection(fileName);
            if(!inCollection) {
                Log.d(TAG, "not currently in the collection");
                String localFileName  = remoteStorage.downloadFile(remotePath, fileName);
                Song newSong = new Song(localFileName);
                newSong.addToPlayList(this);
                newSong.refreshLastUpdate();
                MusicCollectionManager.getInstance().addToSongCollection(newSong);
            }
            if(inCollection) {
                Log.d(TAG, "is in Collection but does it need to be renewed");
                Song oldSong = MusicCollectionManager.getInstance().getSongByFileName(fileName);
                remoteStorage.downloadFileIfNewerVersion(remotePath, fileName, oldSong.getLastUpdate());
            }
            //TODO: delete those that are no longer in remote storage
            Log.d(TAG, "now checking if a song needs to be deleted");
            List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(this);
            for(Song song : songsInPlayList) {
                Log.d(TAG, song.getFileName());
                if(!listOfFileNames.contains(song.getFileName())) {
                    Log.d(TAG, "This Song needs to be removed from this Playlist");
                    song.removeFromPlayList(this);
                    if(song.isToBeDeleted()) {
                        remoteStorage.deleteSongFromLocalStorage(song);
                    }
                }
            }
            MusicCollectionManager.getInstance().removeSongsThatAreToBeDeleted();

        }
    }

    public String getName() {
        return remotePath;
    }
}

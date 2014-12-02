package de.lukeslog.trashplay.playlist;

import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;

import de.lukeslog.trashplay.cloudstorage.CloudStorage;
import de.lukeslog.trashplay.cloudstorage.DropBox;
import de.lukeslog.trashplay.constants.TrashPlayConstants;

/**
 * PlayList implements a PlayList which is a music folder in a cloud storage
 */
public class PlayList {

    CloudStorage remoteStorage;
    String remotePath;
    boolean activated=false;

    public static final String TAG = TrashPlayConstants.TAG;

    PlayList(CloudStorage remoteStorage, String remotePath) {
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
                //TODO get those that have changed
                Song oldSong = MusicCollectionManager.getInstance().getSongByFileName(fileName);
                remoteStorage.downloadFileIfNewerVersion(remotePath, fileName, oldSong.getLastUpdate());
            }

            //TODO: delete those that are no longer in remote storage
        }
    }

    public String getName() {
        return remotePath;
    }
}

package de.lukeslog.trashplay.playlist;

import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.service.TrashPlayService;
import io.realm.Realm;

public class PlayListFileSynchronizer {

    public static final String TAG = TrashPlayConstants.TAG;

    public static void synchronize(PlayList playList) throws Exception {
        Log.d(TAG, "Synchronize Files from PlayList");
        MusicCollectionManager.getInstance().determineNumberOfActivatedPlayLists();
        StorageManager storage = StorageManager.getStorage(playList.getRemoteStorage());
        ArrayList<String> listOfFileNames = storage.
                getFileNameListWithEndings(MusicCollectionManager.getListOfAllowedFileEndings(), playList.getRemotePath());
        Log.d(TAG, "PlayList has gotten a list of " + listOfFileNames.size() + " names");
        for (String fileName : listOfFileNames) {
            Log.d(TAG, "-> " + fileName);
            boolean inCollection = MusicCollectionManager.getInstance().isSongInCollection(fileName);
            if (!inCollection) {
                Log.d(TAG, "not currently in the collection");
                String localFileName = storage.downloadFile(playList.getRemotePath(), fileName);
                Realm realm = Realm.getInstance(TrashPlayService.getContext());
                realm.beginTransaction();
                Song newSong = realm.createObject(Song.class);
                newSong.setFileName(localFileName);
                newSong.setInActiveUse(true);
                SongHelper.getMetaData(newSong);
                SongHelper.addSongToPlayList(newSong, playList);
                SongHelper.refreshLastUpdate(newSong);
                realm.commitTransaction();
            }
            if (inCollection) {
                Log.d(TAG, "is in Collection but does it need to be renewed");
                Song oldSong = MusicCollectionManager.getInstance().getSongByFileName(fileName);
                storage.downloadFileIfNewerVersion(playList.getRemotePath(), fileName, new DateTime(oldSong.getLastUpdate()));
            }
            //TODO: delete those that are no longer in remote storage
            Log.d(TAG, "now checking if a song needs to be deleted");
            List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(playList);
            for (Song song : songsInPlayList) {
                Log.d(TAG, song.getFileName());
                if (!listOfFileNames.contains(song.getFileName())) {
                    Log.d(TAG, "This Song needs to be removed from this Playlist");
                    SongHelper.removeFromPlayList(song, playList);
                }
            }
            MusicCollectionManager.getInstance().removeSongsThatAreToBeDeleted();
        }
    }

    public static void activated(PlayList playList, boolean b) {
        List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(playList);
        Realm realm = Realm.getInstance(TrashPlayService.getContext());
        realm.beginTransaction();
        playList.setActivated(b);
        for (Song song : songsInPlayList) {
            List<PlayList> isInPlayLists = song.getPlayLists();
            boolean active = false;
            for(PlayList playListofSong : isInPlayLists) {
                if(playListofSong.isActivated()) {
                    active=true;
                }
            }
            song.setInActiveUse(active);
        }
        realm.commitTransaction();
        MusicCollectionManager.getInstance().determineNumberOfActivatedPlayLists();
    }
}

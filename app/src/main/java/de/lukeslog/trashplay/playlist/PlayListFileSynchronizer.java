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
    public static boolean sync=false;

    public static void synchronize(PlayList playList) throws Exception {
        Log.d(TAG, "Synchronize Files from PlayList");
        if(!sync) {
            try {
                sync = true;
                MusicCollectionManager.getInstance().determineNumberOfActivatedPlayLists();
                StorageManager storage = StorageManager.getStorage(playList.getRemoteStorage());
                ArrayList<String> listOfFileNames = storage.
                        getFileNameListWithEndings(MusicCollectionManager.getListOfAllowedFileEndings(), playList.getRemotePath());
                Log.d(TAG, "PlayList has gotten a list of " + listOfFileNames.size() + " names");
                ArrayList<String> newSongs = new ArrayList<String>();
                for (String fileName : listOfFileNames) {
                    Log.d(TAG, "-> " + fileName);
                    boolean inCollection = MusicCollectionManager.getInstance().isSongInCollection(fileName);
                    if (!inCollection) {

                        if (TrashPlayService.wifi) {
                            String localFileName = storage.downloadFile(playList.getRemotePath(), fileName);
                            Log.d(TAG, "back in the file Synchronizer");
                            newSongs.add(localFileName);
                        }
                    }
                }
                Realm realm = Realm.getInstance(TrashPlayService.getContext());
                for (String localFileName : newSongs) {
                    try {
                        Log.d(TAG, "try to create objects");
                        realm.beginTransaction();
                        Log.d(TAG, "1");
                        Song newSong = realm.createObject(Song.class);
                        Log.d(TAG, "2");
                        newSong.setFileName(localFileName);
                        Log.d(TAG, "4");
                        SongHelper.getMetaData(newSong);
                        Log.d(TAG, "5");
                        SongHelper.addSongToPlayList(newSong, playList);
                        Log.d(TAG, "6");
                        SongHelper.refreshLastUpdate(newSong);
                        Log.d(TAG, "7");
                        newSong.getFileName();
                        Log.d(TAG, "8");
                        newSong.setInActiveUse(true);
                        Log.d(TAG, "3");
                        realm.commitTransaction();
                        Log.d(TAG, "9");
                    } catch (Exception e) {
                        Log.e(TAG, "EXCEPTION WHEN CREATING SONG");
                        realm.cancelTransaction();
                    }
                }
                for (String localFileName : newSongs) {
                    boolean inCollection = MusicCollectionManager.getInstance().isSongInCollection(localFileName);
                    if (inCollection) {
                        Log.d(TAG, "is in Collection but does it need to be renewed");
                        if (TrashPlayService.wifi) {
                            Song oldSong = MusicCollectionManager.getInstance().getSongByFileName(localFileName);
                            String x = storage.downloadFileIfNewerVersion(playList.getRemotePath(), localFileName, new DateTime(oldSong.getLastUpdate()));
                            if (!x.equals("")) {
                                try {
                                    realm.beginTransaction();
                                    oldSong.setLastUpdate(new DateTime().getMillis());
                                    realm.commitTransaction();
                                    Log.d(TAG, "9");
                                } catch (Exception e) {
                                    Log.e(TAG, "EXCEPTION WHEN CREATING SONG");
                                    realm.cancelTransaction();
                                }
                            }
                        }
                    }
                }
                //TODO: delete those that are no longer in remote storage
                Log.d(TAG, "now checking if a song needs to be deleted");
                List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(playList);
                for (
                        Song song
                        : songsInPlayList)

                {
                    Log.d(TAG, song.getFileName());
                    if (!listOfFileNames.contains(song.getFileName())) {
                        Log.d(TAG, "This Song needs to be removed from this Playlist");
                        SongHelper.removeFromPlayList(song, playList);
                    }
                }

                MusicCollectionManager.getInstance().

                        removeSongsThatAreToBeDeleted();
                sync = false;
            } catch(Exception e) {
                sync = false;
                throw e;
            }

        }

    }

    public static void activated(PlayList playList, boolean b) {
        List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(playList);
        Realm realm = Realm.getInstance(TrashPlayService.getContext());
        try {
            realm.beginTransaction();
            Log.d(TAG, "setActivated to " + b);
            playList.setActivated(b);
            for (Song song : songsInPlayList) {
                List<PlayList> isInPlayLists = song.getPlayLists();
                boolean active = false;
                for (PlayList playListOfSong : isInPlayLists) {
                    if (playListOfSong.isActivated()) {
                        active = true;
                    }
                }
                song.setInActiveUse(active);
            }
            realm.commitTransaction();
        } catch (Exception e) {
            realm.cancelTransaction();
        }
        MusicCollectionManager.getInstance().determineNumberOfActivatedPlayLists();
    }
}

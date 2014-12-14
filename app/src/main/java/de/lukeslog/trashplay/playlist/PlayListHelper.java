package de.lukeslog.trashplay.playlist;

import android.util.Log;

import com.activeandroid.query.Select;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.service.TrashPlayServiceNotRunningException;
import de.lukeslog.trashplay.support.Logger;

public class PlayListHelper {

    public static final String TAG = TrashPlayConstants.TAG;
    public static boolean sync = false;

    //I hate this so much...
    public static String getPlayListEncodingString(PlayList playList){
        return ","+playList.getRemoteStorage()+"/"+playList.getRemotePath()+",";
    }

    public static void synchronize(PlayList playList) throws Exception {
        Logger.d(TAG, "Synchronize Files from PlayList");
        if (!sync) {
            try {
                sync = true;
                MusicCollectionManager.getInstance().determineNumberOfActivatedPlayLists();
                StorageManager storage = StorageManager.getStorage(playList.getRemoteStorage());
                ArrayList<String> listOfFileNames = storage.
                        getFileNameListWithEndings(MusicCollectionManager.getListOfAllowedFileEndings(), playList.getRemotePath());
                Logger.d(TAG, "PlayList has gotten a list of " + listOfFileNames.size() + " names");
                ArrayList<String> newSongs = new ArrayList<String>();
                for (String fileName : listOfFileNames) {
                    Logger.d(TAG, "-> " + fileName);
                    boolean inCollection = SongHelper.isSongInCollection(fileName);
                    if (!inCollection) {

                        if (TrashPlayService.wifi) {
                            String localFileName = storage.downloadFile(playList.getRemotePath(), fileName);
                            Logger.d(TAG, "back in the file Synchronizer");
                            SongHelper.createSong(localFileName, playList);
                            newSongs.add(localFileName);
                            SongHelper.determineNumberOfViableSongs();
                        }
                    }
                    else
                    {
                        SongHelper.addSongToPlayList(SongHelper.getSongByFileName(fileName), playList);
                    }
                }
                Logger.d(TAG, "Donw woth downloading in the Song Helper... lets try to create mp3s");
                for (String localFileName : newSongs) {
                    boolean inCollection = SongHelper.isSongInCollection(localFileName);
                    if (inCollection) {
                        Logger.d(TAG, "is in Collection but does it need to be renewed");
                        if (TrashPlayService.wifi) {
                            Song oldSong = SongHelper.getSongByFileName(localFileName);
                            String x = storage.downloadFileIfNewerVersion(playList.getRemotePath(), localFileName, new DateTime(oldSong.getLastUpdate()));
                            if (!x.equals("")) {
                                try {
                                    oldSong.setLastUpdate(new DateTime().getMillis());
                                    oldSong.save();
                                    Logger.d(TAG, "9");
                                } catch (Exception e) {
                                    Logger.e(TAG, "EXCEPTION WHEN RENEWING SONG");
                                }
                            }
                        }
                    }
                }
                //TODO: delete those that are no longer in remote storage
                Logger.d(TAG, "now checking if a song needs to be deleted");
                List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(playList);
                for (Song song: songsInPlayList) {
                    Logger.d(TAG, song.getFileName());
                    if (!listOfFileNames.contains(song.getFileName())) {
                        Logger.d(TAG, "This Song needs to be removed from this Playlist");
                        SongHelper.removeFromPlayList(song, playList);
                    }
                }

                SongHelper.removeSongsThatAreToBeDeleted();
                sync = false;
            } catch (Exception e) {
                sync = false;
                throw e;
            }
        }
    }

    static void createNewPlayList(StorageManager storageManager, String path) throws TrashPlayServiceNotRunningException {
        if (TrashPlayService.serviceRunning()) {
            try {
                PlayList playlist = new PlayList();
                playlist.setRemoteStorage(storageManager.getStorageType());
                playlist.setRemotePath(path);
                playlist.setActivated(true);
                playlist.save();
            } catch (Exception e) {
                Logger.e(TAG, "Create PlayList had a problem.");
            }
        }
        Logger.e(TAG, "Trashplayservice not running?");
        throw new TrashPlayServiceNotRunningException();
    }

    public static void activated(PlayList playList, boolean b) {
        List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(playList);
        try {
            Logger.d(TAG, "setActivated to " + b);
            playList.setActivated(b);
            playList.save();
            for (Song song : songsInPlayList) {
                List<PlayList> isInPlayLists = SongHelper.getAllPlayListsFromSong(song);
                Logger.d(TAG, "This song is in "+isInPlayLists.size()+" playlists.");
                boolean active = false;
                for (PlayList playListOfSong : isInPlayLists) {
                    if (playListOfSong.isActivated()) {
                        Logger.d(TAG, "song stays active");
                        active = true;
                    }
                }
                song.setInActiveUse(active);
                song.save();
            }
        } catch (Exception e) {
            Logger.e(TAG, "error in activaeted in PlayListHelper");
        }
        MusicCollectionManager.getInstance().determineNumberOfActivatedPlayLists();
    }

    public static List<PlayList> getAllPlayLists() {
        Logger.d(TAG, "getAllPalyLists");
        List<PlayList> playlists = new ArrayList<PlayList>();
        try {
            if (TrashPlayService.serviceRunning()) {
                playlists = new Select().from(PlayList.class).execute();
            }
            Logger.d(TAG, "olol");
        } catch (Exception e) {
            Logger.e(TAG, "Invalid Databse? Delete everything? 34");

        }
        return playlists;
    }
}

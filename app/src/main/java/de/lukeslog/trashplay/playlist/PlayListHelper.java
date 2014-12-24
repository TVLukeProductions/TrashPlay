package de.lukeslog.trashplay.playlist;

import android.content.ClipData;
import android.util.Log;

import com.activeandroid.query.Delete;
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
        Logger.d(TAG, "Synchronize Files from PlayList "+playList.getRemotePath()+" ("+playList.getRemoteStorage()+")");
        if (!sync) {
            try {
                sync = true;
                Logger.d(TAG, "PlayLIstHelper 1");
                MusicCollectionManager.getInstance().determineNumberOfActivatedPlayLists();
                Logger.d(TAG, "PlayLIstHelper 2 "+playList.getRemoteStorage());
                final StorageManager storage = StorageManager.getStorage(playList.getRemoteStorage());
                if(storage!=null) {
                    Logger.d(TAG, "PlayLIstHelper 3");
                    ArrayList<String> listOfFileNames = storage.
                            getFileNameListWithEndings(MusicCollectionManager.getListOfAllowedFileEndings(), playList.getRemotePath());
                    Logger.d(TAG, "PlayList has gotten a list of " + listOfFileNames.size() + " names");
                    ArrayList<String> newSongs = new ArrayList<String>();
                    ArrayList<String> songFileNames = getAllSongFileNames();
                    for (String fileName : listOfFileNames) {
                        boolean inCollection = (songFileNames.contains(fileName) || songFileNames.contains(fileName.replace(StorageManager.PATH_CHRISTMAS + "/", "")));
                        if (!inCollection) {
                            if (TrashPlayService.wifi) {
                                String localFileName = storage.downloadFile(playList.getRemotePath(), fileName);
                                Logger.d(TAG, "-->" + localFileName);
                                SongHelper.createSong(localFileName, playList);
                                newSongs.add(localFileName);
                                SongHelper.determineNumberOfViableSongs();
                            }
                        } else {
                            SongHelper.addSongToPlayList(SongHelper.getSongByFileName(fileName), playList);
                        }
                    }
                    Logger.d(TAG, "Donw woth downloading in the Song Helper... lets try to create mp3s");
                    songFileNames = getAllSongFileNamesForPlayList(playList);
                    for (String localFileName : songFileNames) {
                        boolean inCollection = (songFileNames.contains(localFileName) || songFileNames.contains(localFileName.replace(StorageManager.PATH_CHRISTMAS + "/", "")));
                        if (inCollection) {
                            Logger.d(TAG, "is in Collection but does it need to be renewed");
                            final Song oldSong = SongHelper.getSongByFileName(localFileName);
                            if (TrashPlayService.wifi) {
                                if (!SongHelper.localFileExists(oldSong)) {
                                    oldSong.setLastUpdate(0l);
                                }
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
                            //TODO: Remove after a few versions, if all this stuff gets set automatically
                            new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        SongHelper.metaDataUpdate(oldSong);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                    }
                    //TODO: delete those that are no longer in remote storage
                    Logger.d(TAG, "now checking if a song needs to be deleted");
                    List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(playList);
                    for (Song song : songsInPlayList) {
                        if (!listOfFileNames.contains(song.getFileName()) && !listOfFileNames.contains(StorageManager.PATH_CHRISTMAS + "/" + song.getFileName())) {
                            Logger.d(TAG, "This Song needs to be removed from this Playlist "+song.getSongName());
                            SongHelper.removeFromPlayList(song, playList);
                        } else {

                        }
                    }

                    SongHelper.removeSongsThatAreToBeDeleted();
                    setActivated(playList, playList.isActivated());
                    sync = false;
                }
                sync = false;
            } catch (Exception e) {
                sync = false;
                throw e;
            }
        }
    }

    private static ArrayList<String> getAllSongFileNamesForPlayList(PlayList playList) {
        ArrayList<String> songFileNames = new ArrayList<String>();
        List<Song> songsInPlayList = new ArrayList<Song>();
        List<Song> songs =SongHelper.getAllSongs();
        String playListString = PlayListHelper.getPlayListEncodingString(playList);
        for(Song song : songs){
            if(song.getPlayLists().contains(playListString)){
                songsInPlayList.add(song);
            }
        }
        for (Song song : songsInPlayList) {
            songFileNames.add(song.getFileName());
        }
        return songFileNames;
    }

    private static ArrayList<String> getAllSongFileNames() {
        List<Song> songCollection = SongHelper.getAllSongs();
        ArrayList<String> songFileNames = new ArrayList<String>();
        for (Song song : songCollection) {
            songFileNames.add(song.getFileName());
        }
        return songFileNames;
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

    public static void setActivated(PlayList playList, boolean b) {
        List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(playList);
        try {
            Logger.d(TAG, playList.getRemotePath()+" setActivated to " + b);
            playList.setActivated(b);
            playList.save();
            for (Song song : songsInPlayList) {
                List<PlayList> isInPlayLists = SongHelper.getAllPlayListsFromSong(song);
                boolean active = false;
                for (PlayList playListOfSong : isInPlayLists) {
                    if (playListOfSong.isActivated()) {
                        active = true;
                    }
                }
                SongHelper.setActive(song, active);
            }
        } catch (Exception e) {
            Logger.e(TAG, "error in activaeted in PlayListHelper");
        }
        MusicCollectionManager.getInstance().determineNumberOfActivatedPlayLists();
        SongHelper.determineNumberOfViableSongs();
    }

    public static List<PlayList> getAllPlayLists() {
        List<PlayList> playlists = new ArrayList<PlayList>();
        try {
            if (TrashPlayService.serviceRunning()) {
                playlists = new Select().from(PlayList.class).execute();
            }
        } catch (Exception e) {
            Logger.e(TAG, "Invalid Databse? Delete everything? 34");

        }
        return playlists;
    }

    public static int getNumberOfSongsInPlayList(PlayList playList) {
        int n=0;
        List<Song> songs =SongHelper.getAllSongs();
        String playListString = PlayListHelper.getPlayListEncodingString(playList);
        for(Song song : songs){
            if(song.getPlayLists().contains(playListString)){
                n++;
            }
        }
        return n;
    }

    public static void removePlaylist(PlayList playlist){
        Logger.d(TAG, "remove Playlist called");
        removeSongsFromPlayList(playlist);
        new Delete().from(PlayList.class).where("Id = ?", playlist.getId()).execute();
    }

    private static void removeSongsFromPlayList(PlayList playlist) {
        List<Song> songsInPlayList = MusicCollectionManager.getInstance().getSongsByPlayList(playlist);
        for(Song song : songsInPlayList){
            SongHelper.removeFromPlayList(song, playlist);
        }
    }
}

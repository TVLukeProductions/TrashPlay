package de.lukeslog.trashplay.cloudstorage;

import android.os.Environment;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.playlist.SongHelper;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;

public abstract class StorageManager {

    public static final String STORAGE_TYPE_DROPBOX = "DropBox";
    public static final String STORAGE_TYPE_GDRIVE = "GDrive";
    public static final String STORAGE_TYPE_LOCAL ="LocalDrive";

    public static final String PATH_CHRISTMAS = "Christmas";

    public static final String TAG = TrashPlayConstants.TAG;

    public static boolean syncInProgress = false;
    public static final String LOCAL_STORAGE = Environment.getExternalStorageDirectory().getPath() + "/Music/TrashPlay/";

    public static final int REQUEST_LINK_TO_DBX = 56;

    public void synchronize(final String path) {
        if (TrashPlayService.wifi) {
            if (!syncInProgress) {
                new Thread(new Runnable() {
                    public void run() {
                        setSyncInProgress(true);
                        synchronizeRemoteFiles(path);
                        setSyncInProgress(false);
                    }
                }).start();
            }
        }
    }

    protected File findOrCreateLocalTrashPlayMusicFolder() {
        try {
            File folder = new File(LOCAL_STORAGE);
            folder.mkdirs();
            File folder2 = new File(LOCAL_STORAGE + "Local");
            folder2.mkdirs();
            return folder;
        } catch(Exception e){
            Logger.e(TAG, "findOrCreateLocalTrashPlayMusicFolder() EXCEPTION "+e);
        }
        return null;
    }


    private void setSyncInProgress(boolean syncing) {
        syncInProgress = syncing;
    }

    protected abstract void synchronizeRemoteFiles(String path);

    /**
     * Blocking method that will eventually return the list of playlist folders
     *
     * @return List with the folders with a playlist
     * @throws InterruptedException
     */
    public List<String> getAllPlayListFolders() throws Exception {
        Logger.d(TAG, "getAllPlaylist Folders");
        List<String> playListFolders = new ArrayList<String>();
        if (TrashPlayService.wifi && !syncInProgress) {
            Logger.d(TAG, "wifi is");
            while (syncInProgress) {
                Thread.sleep(100);
            }
            Logger.d(TAG, "setSync");
            setSyncInProgress(true);
            try {
                Logger.d(TAG, "search...");
                playListFolders = searchForPlayListFolderInRemoteStorageImplementation();
            } catch (Exception e) {
                setSyncInProgress(false);
                throw e;
            }
            setSyncInProgress(false);
        }
        return playListFolders;
    }

    public String downloadFile(String path, String fileName) throws Exception {
        String result = "";
        while (syncInProgress) {
            Thread.sleep(100);
        }
        setSyncInProgress(true);
        try {
            result = downloadFileFromRemoteStorage(path, fileName);
        } catch (Exception e) {
            setSyncInProgress(false);
            throw e;
        }
        setSyncInProgress(false);
        return result;
    }

    public String downloadFileIfNewerVersion(String path, String fileName, DateTime lastChange) throws Exception {
        String result = "";
        while (syncInProgress) {
            Thread.sleep(100);
        }
        setSyncInProgress(true);
        try {
            result = downloadFileIfNewerVersionFromRemoteStorage(path, fileName, lastChange);
            if(!result.equals("")){
                SongHelper.refreshLastUpdate(fileName);
            }
        } catch (Exception e) {
            setSyncInProgress(false);
            throw e;
        }
        setSyncInProgress(false);
        return result;
    }

    public ArrayList<String> getFileNameListWithEndings(List<String> listOfAllowedFileEndings, String folderPath) throws Exception {
        ArrayList<String> fnl;
        while (syncInProgress) {
            Thread.sleep(100);
        }
        setSyncInProgress(true);
        try {
            fnl = getFileNameListWithEndingsFromRemoteStorage(listOfAllowedFileEndings, folderPath);
        } catch (Exception e) {
            setSyncInProgress(false);
            throw e;
        }
        setSyncInProgress(false);
        return fnl;
    }

    public void updateRadioFile(String path) throws Exception {
        while (syncInProgress) {
            Thread.sleep(100);
        }
        setSyncInProgress(true);
        try {
            updateRadioFileToRemoteStorage(path);
        } catch (Exception e) {
            setSyncInProgress(false);
            throw e;
        }
        setSyncInProgress(false);
    }

    protected abstract void updateRadioFileToRemoteStorage(String path) throws Exception;

    protected abstract void deleteOldRadioFiles(String path) throws Exception;

    public abstract boolean isConnected();

    protected abstract List<String> searchForPlayListFolderInRemoteStorageImplementation() throws Exception;

    public abstract ArrayList<String> getFileNameListWithEndingsFromRemoteStorage(List<String> listOfAllowedFileEndings, String folderPath) throws Exception;

    protected abstract String downloadFileFromRemoteStorage(String path, String fileName) throws Exception;

    protected abstract String downloadFileIfNewerVersionFromRemoteStorage(String path, String fileName, DateTime lastChange) throws Exception;

    public abstract int getIconResourceNotConnected();

    public abstract int getIconResourceConnected();

    public abstract String returnUniqueReadableName();

    public abstract int menuItem();

    public abstract String getStorageType();

    public static void deleteSongFromLocalStorage(Song song) {
        Logger.d(TAG, "delete from Local Storage "+song.getFileName());
        File songFile = new File(LOCAL_STORAGE + song.getFileName());
        System.gc();
        songFile.delete();
        System.gc();
        Logger.d(TAG, "Does the song still exist?" + songFile.exists());
    }

    public boolean isSyncInProgress() {
        return syncInProgress;
    }

    public static boolean doesFileExists(Song song) {
        Logger.d(TAG, "Song Filename given as "+song.getFileName());
        File f = new File(StorageManager.LOCAL_STORAGE + song.getFileName());
        boolean localExists = f.exists();
        if(localExists){
            localExists = f.length()>0;
        }
        Logger.d(TAG, "" + localExists);
        if (!localExists) {
            Logger.d(TAG, "nope!");
            song.setToBeDeleted(true);
            SongHelper.resetPlayList(song);
        }
        return localExists;
    }

    public static StorageManager getStorage(String remoteStorage) {
        Logger.d(TAG, "getStorage "+remoteStorage);
        List<StorageManager> services = CloudSynchronizationService.getRegisteredCloudStorageServices();
        Logger.d(TAG, "SERVICES? "+services.size());
        for(StorageManager service : services){
            if(service.getStorageType().equals(remoteStorage)){
                return service;
            }
        }
        Logger.d(TAG, "RETURN NULL");
        return null;
    }

    public abstract void getRadioStations(String folderPath) throws Exception;

    public abstract void resetSyncInProgress();

    protected boolean hasAllowedFileEnding(List<String> listOfAllowedFileEndings, String filename) {
        for (String ending : listOfAllowedFileEndings) {
            if (filename.endsWith(ending)) {
                return true;
            }
        }
        return false;
    }
}

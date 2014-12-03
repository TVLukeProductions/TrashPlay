package de.lukeslog.trashplay.cloudstorage;

import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;

import com.dropbox.client2.exception.DropboxException;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.service.TrashPlayService;

public abstract class CloudStorage{

    public static final String TAG = TrashPlayConstants.TAG;

    public static boolean syncInProgress =false;
    public static final String LOCAL_STORAGE = Environment.getExternalStorageDirectory().getPath() + "/Music/TrashPlay/";

    public static final int REQUEST_LINK_TO_DBX = 56;

    public void synchronize(final String path) {
        if(TrashPlayService.wifi) {
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
        File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/Music/TrashPlay");
        folder.mkdirs();
        return folder;
    }


    private void setSyncInProgress(boolean syncing)
    {
        Log.d(TAG, "SSSSSSSSSSSSYNC = "+syncing);
        syncInProgress =syncing;
    }

    protected abstract void synchronizeRemoteFiles(String path);

    /**
     * Blocking method that will eventually return the list of playlist folders
     *
     * @return List with the folders with a playlist
     * @throws InterruptedException
     */
    public List<String> getAllPlayListFolders() throws Exception {
        List<String> playListFolders = new ArrayList<String>();
        if(TrashPlayService.wifi) {
            while(syncInProgress) {
                Thread.sleep(100);
                Log.d(TAG, "SLEEP1");
            }
            setSyncInProgress(true);
            playListFolders = searchForPlayListFolderInRemoteStorageImplementation();
            setSyncInProgress(false);
        }
        return playListFolders;
    }

    public String downloadFile(String path, String fileName) throws Exception {
        while(syncInProgress) {
            Thread.sleep(100);
            Log.d(TAG, "SLEEP2");
        }
        setSyncInProgress(true);
        String result = downloadFileFromRemoteStorage(path, fileName);
        setSyncInProgress(false);
        return result;
    }

    public String downloadFileIfNewerVersion(String path, String fileName, DateTime lastChange) throws Exception {
        while(syncInProgress) {
            Thread.sleep(100);
            Log.d(TAG, "SLEEP3");
        }
        setSyncInProgress(true);
        String result = downloadFileIfNewerVersionFromRemoteStorage(path, fileName, lastChange);
        setSyncInProgress(false);
        return result;
    }

    public ArrayList<String> getFileNameListWithEndings(List<String> listOfAllowedFileEndings, String folderPath) throws Exception {
        ArrayList<String> fnl;
        while(syncInProgress) {
            Thread.sleep(100);
            Log.d(TAG, "SLEEP4");
        }
        setSyncInProgress(true);
        fnl = getFileNameListWithEndingsFromRemoteStorage(listOfAllowedFileEndings, folderPath);
        setSyncInProgress(false);
        return fnl;
    }

    public abstract boolean isConnected();

    protected abstract List<String> searchForPlayListFolderInRemoteStorageImplementation() throws Exception;

    public abstract ArrayList<String> getFileNameListWithEndingsFromRemoteStorage(List<String> listOfAllowedFileEndings, String folderPath) throws Exception;

    protected abstract String downloadFileFromRemoteStorage(String path, String fileName) throws Exception;

    protected abstract String downloadFileIfNewerVersionFromRemoteStorage(String path, String fileName, DateTime lastChange) throws Exception;

    public abstract int getIconResouceNotConnected();

    public abstract int getIconResourceConnected();

    public abstract String returnUniqueReadableName();

    public abstract int menuItem();

    public void deleteSongFromLocalStorage(Song song) {
        File songFile = new File(LOCAL_STORAGE+song.getFileName());
        System.gc();
        songFile.delete();
        System.gc();
    }

    public boolean isSyncInProgress() {
        return syncInProgress;
    }
}

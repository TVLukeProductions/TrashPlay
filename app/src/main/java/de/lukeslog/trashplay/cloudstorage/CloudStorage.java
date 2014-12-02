package de.lukeslog.trashplay.cloudstorage;

import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.MenuItem;

import com.dropbox.client2.exception.DropboxException;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.service.TrashPlayService;

public abstract class CloudStorage{

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
            }
            return searchForPlayListFolderInRemoteStorageImplementation();
        }
        return playListFolders;
    }

    public abstract boolean isConnected();

    protected abstract List<String> searchForPlayListFolderInRemoteStorageImplementation() throws Exception;

    public abstract ArrayList<String> getFileNameListWithEndings(List<String> listOfAllowedFileEndings, String folderPath) throws Exception;

    public abstract String downloadFile(String path, String fileName) throws Exception;

    public abstract String downloadFileIfNewerVersion(String path, String fileName, DateTime lastChange) throws Exception;

    public abstract int getIconResouceNotConnected();

    public abstract int getIconResourceConnected();

    public abstract String returnUniqueReadableName();

    public abstract int menuItem();
}

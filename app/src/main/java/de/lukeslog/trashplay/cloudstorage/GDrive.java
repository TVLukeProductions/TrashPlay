package de.lukeslog.trashplay.cloudstorage;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class GDrive extends StorageManager {

    private static GDrive instance = null;
    public static final String STORAGE_TYPE = StorageManager.STORAGE_TYPE_GDRIVE;

    private GDrive() {

    }

    public static synchronized GDrive getInstance() {
        if (instance == null) {
            instance = new GDrive();
        }
        return instance;
    }

    @Override
    protected void synchronizeRemoteFiles(String path) {

    }

    @Override
    protected void updateRadioFileToRemoteStorage(String path) throws Exception {

    }

    @Override
    protected void deleteOldRadioFiles() throws Exception {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    protected List<String> searchForPlayListFolderInRemoteStorageImplementation() throws Exception {
        return null;
    }

    @Override
    public ArrayList<String> getFileNameListWithEndingsFromRemoteStorage(List<String> listOfAllowedFileEndings, String folderPath) throws Exception {
        return null;
    }

    @Override
    protected String downloadFileFromRemoteStorage(String path, String fileName) throws Exception {
        return null;
    }

    @Override
    protected String downloadFileIfNewerVersionFromRemoteStorage(String path, String fileName, DateTime lastChange) throws Exception {
        return null;
    }

    @Override
    public int getIconResourceNotConnected() {
        return 0;
    }

    @Override
    public int getIconResourceConnected() {
        return 0;
    }

    @Override
    public String returnUniqueReadableName() {
        return null;
    }

    @Override
    public int menuItem() {
        return 0;
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    @Override
    public void getRadioStations(String folderPath) throws Exception {

    }

    @Override
    public void resetSyncInProgress() {

    }
}

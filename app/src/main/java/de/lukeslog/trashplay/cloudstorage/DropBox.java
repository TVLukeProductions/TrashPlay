package de.lukeslog.trashplay.cloudstorage;

import android.content.SharedPreferences;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.SettingsConstants;
import de.lukeslog.trashplay.support.TrashPlayUtils;

public class DropBox extends StorageManager {

    public static final String STORAGE_TYPE = StorageManager.STORAGE_TYPE_DROPBOX;

    final static private Session.AccessType ACCESS_TYPE = Session.AccessType.DROPBOX;
    private static DropboxAPI<AndroidAuthSession> mDBApi;

    private static DropBox instance = null;

    private DropBox() {

    }

    public static synchronized DropBox getInstance() {
        if (instance == null) {
            instance = new DropBox();
        }
        return instance;
    }

    @Override
    protected void synchronizeRemoteFiles(String path) {
        synchronizeDropBox(path);
    }

    @Override
    protected List<String> searchForPlayListFolderInRemoteStorageImplementation() throws Exception {
        Logger.d(TAG, "DropBoxsearch...");
        List<String> playListFolders = new ArrayList<String>();
        if (mDBApi != null) {
            Entry dropBoxDir1 = mDBApi.metadata("/", 0, null, true, null);
            if (dropBoxDir1.isDir) {
                for (Entry topFolder : dropBoxDir1.contents) {
                    Logger.d(TAG, "" + topFolder.fileName());
                    if (topFolder.isDir) {
                        Entry folder = mDBApi.metadata("/" + topFolder.fileName(), 0, null, true, null);
                        if (isAPlayListFolder(folder)) {
                            playListFolders.add(topFolder.fileName());
                            getRadioStations(topFolder);
                        }
                    }
                }
            }
        }
        return playListFolders;
    }

    @Override
    public void getRadioStations(String folderPath) throws Exception {
        Logger.d(TAG, "getRadioStations...");
        String givenPath = "/" + folderPath;
        if (mDBApi == null) {
            getDropBoxAPI();
        }
        Logger.d(TAG, "olol--");
        Entry folder = mDBApi.metadata(givenPath, 0, null, true, null);
        Logger.d(TAG, "--lolo");
        getRadioStations(folder);
    }

    private void getRadioStations(Entry folder) throws DropboxException {
        Logger.d(TAG, "getRadioStationsRemote...");
        checkAndCreateNecessarySubFolders(folder);
        Logger.d(TAG, "getRadioStationsRemote...");
        String stationNames = "";
        Entry radioFolder = mDBApi.metadata("/" + folder.fileName() + "/Radio", 0, null, true, null);
        Logger.d(TAG, "x");
        List<Entry> possibleStations = radioFolder.contents;
        Logger.d(TAG, "poosible Stations " + possibleStations.size());
        for (Entry possibleStation : possibleStations) {
            if (!possibleStation.isDir && possibleStation.fileName().endsWith(".station")) {
                Logger.d(TAG, "FOUND A RADIO STATION! " + possibleStation.path);
                DateTime now = new DateTime();
                now = now.minusHours(2);
                DateTime actualModificationTime = getDateTimeFromDropBoxModificationTimeString(possibleStation.modified);
                Logger.d(TAG, "ACTUAL MODIFICATION TIME->" + actualModificationTime);
                Logger.d(TAG, "NOW" + now);
                if (now.isAfter(actualModificationTime)) {
                    Logger.d(TAG, "DELETE RADIO? " + possibleStation.path);
                    mDBApi.delete(possibleStation.path);
                } else {
                    stationNames = stationNames + possibleStation.fileName() + "XX88XX88XX";
                    Logger.d(TAG, "Stationnames" + stationNames);
                }
            }
        }
        Logger.d(TAG, "Radiostations " + stationNames);
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        String oldRadioString = settings.getString("Radio_" + STORAGE_TYPE + "_" + folder.fileName(), "");
        boolean radioMode = settings.getBoolean(SettingsConstants.APP_SETTING_RADIO_MODE, false);
        Logger.d(TAG, "oldRadioString: "+oldRadioString);
        SharedPreferences.Editor edit = settings.edit();
        edit.putString("Radio_" + STORAGE_TYPE + "_" + folder.fileName(), stationNames);
        edit.commit();
        String newRadioString = settings.getString("Radio_" + STORAGE_TYPE + "_" + folder.fileName(), "");
        Logger.d(TAG, "newRadioString: "+newRadioString);
        if(!oldRadioString.equals(newRadioString) && !newRadioString.equals("") && !radioMode) {
            TrashPlayService.getContext().createRadioNotification("New Radio Station");
        }
    }

    public void updateRadioFileToRemoteStorage(String path) throws Exception {
        if (TrashPlayService.serviceRunning()) {
            if (mDBApi == null) {
                getDropBoxAPI();
            }
            if (mDBApi != null) {
                Logger.d(TAG, "update radio thing");
                SharedPreferences settings = TrashPlayService.getDefaultSettings();
                String radioName = settings.getString(SettingsConstants.APP_SETTING_RADIO_NAME, "");
                radioName = radioName.replace("_", "");
                Logger.d(TAG, "" + radioName);
                String radioSongs = settings.getString("radioSongs", "");
                if (!radioName.equals("")) {
                    Logger.d(TAG, "updating " + radioName);

                    //create File
                    File tempfolder = new File(LOCAL_STORAGE + "/radio/");
                    tempfolder.mkdirs();
                    OutputStream out = null;
                    File file = new File(LOCAL_STORAGE + "/radio/tempradio.txt");

                    //create that file on the harddrive
                    Logger.d(TAG, "have new file");
                    try {
                        out = new BufferedOutputStream(new FileOutputStream(file));
                    } finally {
                        if (out != null) {
                            out.close();
                        }
                    }

                    //write Data into File
                    Logger.d(TAG, file.getAbsolutePath());
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                        writer.write(radioSongs);
                        writer.close();
                    } catch (IOException e) {
                        Log.e("Exception", "File write failed: " + e.toString());
                    }

                    InputStream stream = new FileInputStream(file);

                    Logger.d(TAG, path);
                    Entry playlistFolder = mDBApi.metadata("/" + path, 0, null, true, null);
                    checkAndCreateNecessarySubFolders(playlistFolder);
                    Entry folder = mDBApi.metadata("/" + path + "/Radio", 0, null, true, null);
                    List<Entry> files = folder.contents;
                    for (Entry entry : files) {
                        Logger.d(TAG, entry.fileName());
                        if (entry.fileName().equals(radioName + ".station")) {
                            Logger.d(TAG, "exists");

                            mDBApi.putFileOverwrite("/" + path + "/Radio/" + radioName + ".station", stream, file.length(), null);
                            return;
                        }
                    }
                    Logger.d(TAG, "/" + path + "/Radio/" + radioName + ".station");
                    Logger.d(TAG, "" + file.length());
                    mDBApi.putFile("/" + path + "/Radio/" + radioName + ".station", stream, file.length(), null, null);
                }
            }
        } else {
            Logger.d(TAG, "Dropbox wasn't ready yet....");
        }
    }

    @Override
    protected void deleteOldRadioFiles() throws Exception {
        //      if(actualModificationTime.plusDays(1).isBefore(new DateTime()) && folder.fileName().endsWith(".station")) {
        //          deleteOldRadioFile();
        //      }
    }

    //TODO: Users should not be calling this directly.
    @Override
    public ArrayList<String> getFileNameListWithEndingsFromRemoteStorage(List<String> listOfAllowedFileEndings, String folderPath) throws Exception {
        return getFileNameFromRemoteStorageAndCreateSubFolders(listOfAllowedFileEndings, folderPath, true);
    }

    private ArrayList<String> getFileNameFromRemoteStorageAndCreateSubFolders(List<String> listOfAllowedFileEndings, String folderPath, boolean createSubfolders) throws Exception {
        Logger.d(TAG, "getListOfFiles With Allowed Ending from " + folderPath);
        ArrayList<String> fileList = new ArrayList<String>();
        String givenPath = "/" + folderPath;
        if (mDBApi == null) {
            getDropBoxAPI();
        }
        Logger.d(TAG, "olol");
        Entry folder = mDBApi.metadata(givenPath, 0, null, true, null);
        if(createSubfolders) {
            checkAndCreateNecessarySubFolders(folder);
            getRadioStations(folder);
        }
        if (folder.isDir) {
            if(folder.contents!=null) {
                for (Entry file : folder.contents) {
                    if (!file.isDir) {
                        String filename = file.fileName();
                        Logger.d(TAG, filename);
                        boolean allowed = hasAllowedFileEnding(listOfAllowedFileEndings, filename);
                        if (allowed) {
                            fileList.add(filename);
                        }
                    }
                }
            }
        }
        if(TrashPlayUtils.isChristmasTime() && createSubfolders){
            ArrayList<String> christmasSongs = getFileNameFromRemoteStorageAndCreateSubFolders(listOfAllowedFileEndings, folderPath + "/" + PATH_CHRISTMAS, false);
            for(int i=0; i<christmasSongs.size(); i++){
                christmasSongs.set(i, PATH_CHRISTMAS+"/"+christmasSongs.get(i));
            }
            fileList.addAll(christmasSongs);
        }
        return fileList;
    }

    @Override
    public String downloadFileFromRemoteStorage(String path, String fileName) throws Exception {
        return downloadFileIfNewerVersionFromRemoteStorage(path, fileName, null);
    }

    public String downloadFileIfNewerVersionFromRemoteStorage(String path, String fileName, DateTime lastChange) throws Exception {
        if (lastChange == null) {
            lastChange = new DateTime();
            lastChange = lastChange.minusYears(100);
        }
        if (mDBApi == null) {
            getDropBoxAPI();
        }
        findOrCreateLocalTrashPlayMusicFolder();
        String filePath = "/" + path + "/" + fileName;
        Logger.d(TAG, "Download " + filePath+"?");
        try {
            Entry folder = mDBApi.metadata(filePath, 0, null, true, null);
            String modified = folder.modified;
            DateTime actualModificationTime = getDateTimeFromDropBoxModificationTimeString(modified);
            Logger.d(TAG, actualModificationTime.getDayOfMonth() + "." + actualModificationTime.getMonthOfYear() + "." + actualModificationTime.getYear() + " " + actualModificationTime.getHourOfDay() + " " + actualModificationTime.getMinuteOfHour());
            if (actualModificationTime.isAfter(lastChange)) {
                Logger.d(TAG, "redownload...");
                return downloadSpecificFileFromDropBox(fileName, filePath);
            }
            Logger.d(TAG, "Return nothing");
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    @Override
    public int getIconResourceNotConnected() {
        return R.drawable.dropbox2;
    }

    @Override
    public int getIconResourceConnected() {
        return R.drawable.dropbox;
    }

    @Override
    public String returnUniqueReadableName() {
        return "DropBox";
    }

    @Override
    public int menuItem() {
        return R.id.dropbox;
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    @Override
    public void resetSyncInProgress() {
        Logger.d(TAG, "RESET SYNC");
        syncInProgress = false;
    }

    private String downloadSpecificFileFromDropBox(String fileName, String filePath) throws DropboxException {
        Logger.d(TAG, "try to download a file");
        FileOutputStream outputStream = null;
        if(fileName.startsWith(PATH_CHRISTMAS+"/")){
            fileName=fileName.replace(PATH_CHRISTMAS+"/", "");
        }
        File file = new File(LOCAL_STORAGE + fileName);
        Logger.d(TAG, "have new file");
        try {
            outputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG, "EXCEPTION");
        }

        Logger.d(TAG, "filepath " + filePath);
        mDBApi.getFile(filePath, null, outputStream, null);
        Logger.d(TAG, "I guess the download is done...");
        return file.getName();
    }

    protected DateTime getDateTimeFromDropBoxModificationTimeString(String modified) {
        modified = reformateDateString(modified);

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd MM yyyy HH:mm:ss");
        return formatter.parseDateTime(modified);
    }

    private String reformateDateString(String modifiedDate) {
        modifiedDate = modifiedDate.substring(5);
        modifiedDate = modifiedDate.replace("+0000", "");
        modifiedDate = modifiedDate.replace("Jan", "01").replace("Feb", "02").replace("Mar", "03").
                replace("Apr", "04").replace("May", "05").replace("Jun", "06").replace("Jul", "07").
                replace("Aug", "08").replace("Sep", "09").replace("Oct", "10").replace("Nov", "11").
                replace("Dec", "12");
        modifiedDate = modifiedDate.trim();
        return modifiedDate;
    }


    private boolean isAPlayListFolder(Entry folder) throws DropboxException {
        Logger.d(TAG, folder.fileName() + " " + folder.isDir);
        List<Entry> folderContents = folder.contents;
        if (folderContents != null) {
            for (Entry content : folderContents) {
                if (!content.isDir) {
                    Logger.d(TAG, ">" + content.fileName());
                    if (content.fileName().startsWith(".trashplay")) {
                        Logger.d(TAG, "FOOOUND ONE!");
                        checkAndCreateNecessarySubFolders(folder);
                        return true;
                    }
                    if (content.fileName().matches("^[a-zA-Z].*$")) {
                        Logger.d(TAG, "OVER!");
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private void checkAndCreateNecessarySubFolders(Entry folder) throws DropboxException {
        Logger.d(TAG, "check and create: " + folder.fileName());
        boolean radioFolder = false;
        boolean christmasFolder = false;
        List<Entry> folderContents = folder.contents;
        if (folderContents != null) {
            for (Entry content : folderContents) {
                if (content.isDir) {
                    if (content.fileName().equals("Radio")) {
                        radioFolder = true;
                    }
                    if (content.fileName().equals("Christmas")) {
                        christmasFolder = true;
                    }
                }
            }
        }
        String fileName = folder.fileName();
        if (!fileName.startsWith("/")) {
            fileName = "/" + fileName;
        }
        if (!radioFolder) {
            Logger.d(TAG, "create Radio Folder.");
            try {
                mDBApi.createFolder(fileName + "/Radio");
            } catch (Exception e) {
                Logger.e(TAG, "EXCEPTION" + e);
            }
        }
        if (!christmasFolder) {
            Logger.d(TAG, "create ChristmasFolder.");
            try {
                mDBApi.createFolder(fileName + "/Christmas");
            } catch (Exception e) {
                Logger.e(TAG, "EXCEPTION" + e);
            }
        }
    }

    private void synchronizeDropBox(String path) {
        File trashPlayMusicFolder = findOrCreateLocalTrashPlayMusicFolder();

    }

    public static DropboxAPI<AndroidAuthSession> getDropBoxAPI() {
        Logger.d(TAG, "getDropBoxAPI");
        if (mDBApi == null) {
            getDropboxAPI();
            try {
                MusicCollectionManager.getInstance().syncRemoteStorageWithDevice(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mDBApi;
    }

    private static DropboxAPI<AndroidAuthSession> getDropboxAPI() {
        SharedPreferences settings = null;
        if (TrashPlayService.serviceRunning()) {
            settings = TrashPlayService.getContext().settings;
            Logger.d(TAG, "getDPAPI");
            AppKeyPair appKeys = new AppKeyPair(DropBoxConstants.appKey, DropBoxConstants.appSecret);
            AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
            mDBApi = new DropboxAPI<AndroidAuthSession>(session);
            String dbkey = settings.getString("DB_KEY", "");
            String dbsecret = settings.getString("DB_SECRET", "");
            if (dbkey.equals("")) return null;
            AccessTokenPair access = new AccessTokenPair(dbkey, dbsecret);
            mDBApi.getSession().setAccessTokenPair(access);
            return mDBApi;
        } else {
            Logger.d(TAG, "TrashPlayService was not running.");
        }
        return null;
    }

    @Override
    public boolean isConnected() {
        if (TrashPlayService.serviceRunning()) {
            return !TrashPlayService.getContext().settings.getString("DB_KEY", "").equals("");
        } else {
            Logger.d(TAG, "DropBoxisConnected() sez: TrashPlayService not Running");
        }
        return false;
    }

    public static String disconnect() {
        if (TrashPlayService.serviceRunning()) {
            SharedPreferences.Editor edit = TrashPlayService.getContext().settings.edit();
            edit.putString("DB_KEY", "");
            edit.putString("DB_SECRET", "");
            edit.commit();
            return "Disconnected from DropBox";
        }
        return "";
    }

    public static void storeKeys(String key, String secret) throws Exception {
        if (TrashPlayService.serviceRunning()) {

            Boolean previouslyunconnected = !DropBox.getInstance().isConnected();
            SharedPreferences.Editor edit = TrashPlayService.getContext().settings.edit();
            edit.putString("DB_KEY", key);
            edit.putString("DB_SECRET", secret);
            edit.commit();
            if (previouslyunconnected && DropBox.getInstance().isConnected()) { //TODO: This probably should not be in here...
                MusicCollectionManager.getInstance().syncRemoteStorageWithDevice(false);
            }
        }
    }

    public static void authenticate() {
        Logger.d(TAG, "AUTHENTICATE");
        try {
            if (DropBox.getDropBoxAPI().getSession().authenticationSuccessful()) {
                try {
                    DropBox.getDropBoxAPI().getSession().finishAuthentication();
                    Logger.d(TAG, "lol");
                    AccessTokenPair tokens = DropBox.getDropBoxAPI().getSession().getAccessTokenPair();
                    Logger.d(TAG, "tada");
                    DropBox.storeKeys(tokens.key, tokens.secret);

                    CloudSynchronizationService.registerService(DropBox.getInstance());
                } catch (IllegalStateException e) {
                    Logger.e(TAG, "Error authenticating");
                }
            }
        } catch (Exception e) {
            Logger.d(TAG, "probably a null pointer exception from the dropbox...");
        }
    }
}

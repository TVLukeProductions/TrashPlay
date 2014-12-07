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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.service.TrashPlayService;

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
        List<String> playListFolders = new ArrayList<String>();
        Entry dropBoxDir1 = mDBApi.metadata("/", 0, null, true, null);
        if (dropBoxDir1.isDir) {
            for (Entry topFolder : dropBoxDir1.contents) {
                if (topFolder.isDir) {
                    Entry folder = mDBApi.metadata("/" + topFolder.fileName(), 0, null, true, null);
                    if (isAPlayListFolder(folder)) {
                        playListFolders.add(topFolder.fileName());
                    }
                }
            }
        }
        return playListFolders;
    }

    //TODO: Users should not be calling this directly.
    @Override
    public ArrayList<String> getFileNameListWithEndingsFromRemoteStorage(List<String> listOfAllowedFileEndings, String folderPath) throws Exception {
        Log.d(TAG, "getListOfFiles With Allowed Ending");
        ArrayList<String> fileList = new ArrayList<String>();
        String givenPath = "/" + folderPath;
        Entry folder = mDBApi.metadata(givenPath, 0, null, true, null);
        if (folder.isDir) {
            for (Entry file : folder.contents) {
                if (!file.isDir) {
                    String filename = file.fileName();

                    Log.d(TAG, file.path);
                    Log.d(TAG, file.modified);

                    boolean allowed = hasAllowedFileEnding(listOfAllowedFileEndings, filename);
                    if (allowed) {
                        Log.d(TAG, filename);
                        fileList.add(filename);
                    }
                }
            }
        }
        return fileList;
    }

    @Override
    public String downloadFileFromRemoteStorage(String path, String fileName) throws Exception {
        return downloadFileIfNewerVersionFromRemoteStorage(path, fileName, null);
    }

    public String downloadFileIfNewerVersionFromRemoteStorage(String path, String fileName, DateTime lastChange) throws Exception {
        if(lastChange==null) {
            lastChange= new DateTime();
            lastChange = lastChange.minusYears(100);
        }
        createTrashPlayFolderIFNotExisting();
        String filePath = "/"+path + "/" + fileName;
        Entry folder = mDBApi.metadata(filePath, 0, null, true, null);
        String modified = folder.modified;
        DateTime actualModificationTime = getDateTimeFromDropBoxModificationTimeString(modified);
        Log.d(TAG, actualModificationTime.getDayOfMonth()+"."+actualModificationTime.getMonthOfYear()+"."+actualModificationTime.getYear()+" "+actualModificationTime.getHourOfDay()+" "+actualModificationTime.getMinuteOfHour());
        if(actualModificationTime.isAfter(lastChange)) {
            return downloadSpecificFileFromDropBox(fileName, filePath);
        }
        return "";
    }

    @Override
    public int getIconResourceNotConnected() {
        return R.drawable.dropbox;
    }

    @Override
    public int getIconResourceConnected() {
        return R.drawable.dropbox2;
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
        Log.d(TAG, "RESET SYNC");
        syncInProgress=false;
    }

    private String downloadSpecificFileFromDropBox(String fileName, String filePath) throws DropboxException {
        Log.d(TAG, "try to download a file");
        FileOutputStream outputStream = null;
        File file = new File(LOCAL_STORAGE + fileName);
        Log.d(TAG, "have new file");
        try {
            outputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "EXCEPTION");
        }

        Log.d(TAG, "filepath " + filePath);
        mDBApi.getFile(filePath, null, outputStream, null);
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

    private void createTrashPlayFolderIFNotExisting() {
        File folder = new File(LOCAL_STORAGE);
        folder.mkdirs();
    }

    private boolean hasAllowedFileEnding(List<String> listOfAllowedFileEndings, String filename) {
        for (String ending : listOfAllowedFileEndings) {
            if (filename.endsWith(ending)) {
                return true;
            }
        }
        return false;
    }


    private boolean isAPlayListFolder(Entry folder) {
        Log.d(TAG, folder.fileName() + " " + folder.isDir);
        List<Entry> folderContents = folder.contents;
        if (folderContents != null) {
            for (Entry content : folderContents) {
                if (!content.isDir) {
                    Log.d(TAG, ">" + content.fileName());
                    if (content.fileName().startsWith(".trashplay")) {
                        Log.d(TAG, "FOOOUND ONE!");
                        return true;
                    }
                    if (content.fileName().matches("^[a-zA-Z].*$") || content.fileName().matches("^\\d.*$")) {
                        Log.d(TAG, "OVER!");
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private void synchronizeDropBox(String path) {
        File trashPlayMusicFolder = findOrCreateLocalTrashPlayMusicFolder();

    }

    public static DropboxAPI<AndroidAuthSession> getDropBoxAPI() {
        Log.d(TAG, "getDropBoxAPI");
        if (mDBApi == null) {
            getDropboxAPI();
        }
        return mDBApi;
    }

    private static DropboxAPI<AndroidAuthSession> getDropboxAPI() {
        SharedPreferences settings = null;
        if (TrashPlayService.serviceRunning()) {
            settings = TrashPlayService.getContext().settings;
            Log.d(TAG, "getDPAPI");
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
            Log.d(TAG, "TrashPlayService was not running.");
        }
        return null;
    }

    @Override
    public boolean isConnected() {
        if (TrashPlayService.serviceRunning()) {
            return !TrashPlayService.getContext().settings.getString("DB_KEY", "").equals("");
        } else {
            Log.d(TAG, "DropBoxisConnected() sez: TrashPlayService not Running");
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
                MusicCollectionManager.getInstance().syncRemoteStorageWithDevice();
            }
        }
    }

    public static void authenticate() {
        Log.d(TAG, "AUTHENTICATE THE FUCK OUT OF THIS");
        try {
            if (DropBox.getDropBoxAPI().getSession().authenticationSuccessful()) {
                try {
                    DropBox.getDropBoxAPI().getSession().finishAuthentication();
                    Log.d(TAG, "lol");
                    AccessTokenPair tokens = DropBox.getDropBoxAPI().getSession().getAccessTokenPair();
                    Log.d(TAG, "tada");
                    DropBox.storeKeys(tokens.key, tokens.secret);

                    CloudSynchronizationService.registerService(new DropBox());
                } catch (IllegalStateException e) {
                    Log.i("DbAuthLog", " Error authenticating", e);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "probably a null pointer exception from the dropbox...");
        }
    }
}

package de.lukeslog.trashplay.cloudstorage;

import android.util.Log;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.support.Logger;

public class LocalStorage  extends StorageManager{

    public static final String STORAGE_TYPE = StorageManager.STORAGE_TYPE_LOCAL;

    private static LocalStorage instance = null;

    private LocalStorage() {

    }

    public static synchronized LocalStorage getInstance() {
        if (instance == null) {
            instance = new LocalStorage();
        }
        return instance;
    }

    @Override
    protected void synchronizeRemoteFiles(String path) {
        findOrCreateLocalTrashPlayMusicFolder();
    }

    @Override
    protected void updateRadioFileToRemoteStorage(String path) throws Exception {
        //NOTHING
    }

    @Override
    protected void deleteOldRadioFiles(String path) throws Exception {
        //NOTHING
    }

    @Override
    public boolean isConnected() {
        try {
            List<String> x = searchForPlayListFolderInRemoteStorageImplementation();
            return (x.size()>0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected List<String> searchForPlayListFolderInRemoteStorageImplementation() throws Exception {
        Logger.d(TAG, "search for folders in local drive...");
        List<String> playLists = new ArrayList<String>();
        File file = new File(LOCAL_STORAGE+"Local");
        if(file.isDirectory()) {
            playLists = getPlayListFoldersLocally(file);
        }
        Logger.d(TAG, "found "+playLists.size()+" local lists...");
        return playLists;
    }

    private List<String> getPlayListFoldersLocally(File file) {
        List<String> playLists = new ArrayList<String>();
        File[] files = file.listFiles();
        for(File subFile : files){
            if(subFile.isDirectory()){
                playLists.add(subFile.getName());
            }
        }
        return playLists;
    }

    @Override
    public ArrayList<String> getFileNameListWithEndingsFromRemoteStorage(List<String> listOfAllowedFileEndings, String folderPath) throws Exception {
        ArrayList<String> fileList = new ArrayList<String>();
        File file = new File(LOCAL_STORAGE+"Local/"+folderPath);
        Logger.d(TAG, "LOCAL FOLDER? "+file.getAbsolutePath());
        File[] contents = file.listFiles();
        Logger.d(TAG, "FILES..."+contents.length);
        for(File content : contents){
            if(!content.isDirectory()){

                String filename = content.getName();
                Logger.d(TAG, filename);
                boolean allowed = hasAllowedFileEnding(listOfAllowedFileEndings, filename);
                if (allowed) {
                    fileList.add(filename);
                }
            }
        }
        return fileList;
    }

    @Override
    protected String downloadFileFromRemoteStorage(String path, String fileName) throws Exception {
        return downloadFileIfNewerVersionFromRemoteStorage(path, fileName, null);
    }

    @Override
    protected String downloadFileIfNewerVersionFromRemoteStorage(String path, String fileName, DateTime lastChange) throws Exception {
        Logger.d(TAG, "DOWNLOAD "+path+" "+fileName);
        if (lastChange == null) {
            lastChange = new DateTime();
            lastChange = lastChange.minusYears(100);
        }
        Logger.d(TAG, "DOWNLOAD 1");
        findOrCreateLocalTrashPlayMusicFolder();
        Logger.d(TAG, "DOWNLOAD 2");
        File file = new File(LOCAL_STORAGE+"Local/"+path+"/"+fileName);
        Logger.d(TAG, "DOWNLOAD 3");
        long lastModified = file.lastModified();
        Logger.d(TAG, "DOWNLOAD 4");
        DateTime actualModificationTime = new DateTime(lastModified);
        if (actualModificationTime.isAfter(lastChange)) {
            Logger.d(TAG, "redownload...");
            return copyFile(LOCAL_STORAGE + "Local/" + path+"/", fileName, LOCAL_STORAGE);
        }
        return "";
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
        return "Local Storage";
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
        //nothing
    }

    @Override
    public void resetSyncInProgress() {

    }

    private String copyFile(String inputPath, String inputFile, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file (You have now copied the file)
            out.flush();
            out.close();
            out = null;
            return inputFile;

        }  catch (FileNotFoundException fnfe1) {
            Logger.e(TAG, fnfe1.getMessage());
        }
        catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        }
        return "";
    }
}

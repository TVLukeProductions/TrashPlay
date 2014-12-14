package de.lukeslog.trashplay.cloudstorage;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.support.Logger;

//TODO: This may not be a service
public class CloudSynchronizationService extends Service {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    private static ArrayList<StorageManager> registeredCloudStorageServices = new ArrayList<StorageManager>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void updateRegisteredCloudStorageSystems() {
        if (DropBox.getInstance().isConnected()) {
            registerService(DropBox.getInstance());
        }
    }

    public static void registerService(StorageManager cloudStorageService) {

        boolean alreadyRegistered = alreadyRegistered(cloudStorageService);
        if(!alreadyRegistered) {
            registeredCloudStorageServices.add(cloudStorageService);
        }

    }

    private static boolean alreadyRegistered(StorageManager cloudStorageService) {
        for (StorageManager c : registeredCloudStorageServices) {
            if (c.returnUniqueReadableName().equals(cloudStorageService.returnUniqueReadableName())) {
                return true;
            }
        }
        return false;
    }

    public static List<StorageManager> getRegisteredCloudStorageServices() {
        return registeredCloudStorageServices;
    }

    public static boolean atLeastOneCloudStorageServiceIsConnected() {
        for (StorageManager c : registeredCloudStorageServices) {
            if (c.isConnected()) {
                return true;
            }
        }
        return false;
    }

    public static void resetSyncFlag() {
        Logger.d(TAG, "resetAllSyncFlags");
        updateRegisteredCloudStorageSystems();
        for (StorageManager c : registeredCloudStorageServices) {
            c.resetSyncInProgress();
        }
    }
}

package de.lukeslog.trashplay.cloudstorage;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.constants.TrashPlayConstants;

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
        Log.d(TAG, "...");
    }

    public static void updateRegisteredCloudStorageSystems() {
        //TODO: This should probably be the only place to manually go through these classes.
        if (DropBox.getInstance().isConnected()) {
            registerService(DropBox.getInstance());
        }
    }

    public static void registerService(StorageManager cloudStorageService) {
        Log.d(TAG, "register cloudStorage Service" + registeredCloudStorageServices.size());

        boolean alreadyRegistered = alreadyRegsitered(cloudStorageService);
        if(!alreadyRegistered) {
            registeredCloudStorageServices.add(cloudStorageService);
        }

    }

    private static boolean alreadyRegsitered(StorageManager cloudStorageService) {
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
        Log.d(TAG, "at leastOne Cloud Storage Service Connected?");
        Log.d(TAG, "" + registeredCloudStorageServices.size());
        for (StorageManager c : registeredCloudStorageServices) {
            Log.d(TAG, "...");
            if (c.isConnected()) {
                Log.d(TAG, "yes");
                return true;
            }
        }
        Log.d(TAG, "no");
        return false;
    }
}

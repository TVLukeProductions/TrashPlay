package de.lukeslog.trashplay.playlist;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.service.TrashPlayService;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.RealmClass;

@RealmClass
public class PlayList extends RealmObject {

    private String remoteStorage;
    private String remotePath;
    private boolean activated=true;

    @Ignore
    public static final String TAG = TrashPlayConstants.TAG;

    public String getRemoteStorage() {
        return remoteStorage;
    }

    public void setRemoteStorage(String remoteStorage) {
        this.remoteStorage = remoteStorage;
    }


    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }
}

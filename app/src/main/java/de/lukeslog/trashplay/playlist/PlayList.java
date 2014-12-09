package de.lukeslog.trashplay.playlist;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import de.lukeslog.trashplay.constants.TrashPlayConstants;

@Table(name = "PlayList")
public class PlayList extends Model {

    @Column(name = "remoteStorage")
    private String remoteStorage;

    @Column(name = "remotePath")
    private String remotePath;

    @Column(name = "activated")
    private boolean activated=true;

    public static final String TAG = TrashPlayConstants.TAG;


    public PlayList (){

    }

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

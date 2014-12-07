package de.lukeslog.trashplay.playlist;

import android.util.Log;

import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.lukeslog.trashplay.cloudstorage.CloudSynchronizationService;
import de.lukeslog.trashplay.cloudstorage.LocalStorage;
import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.RealmClass;

@RealmClass
public class Song extends RealmObject {

    private RealmList<PlayList> playLists;
    private String songName="";
    private String artist="";
    private String fileName="";
    private long lastUpdate=0l;
    private int durationInSeconds = 0;
    private int plays = 0;

    private boolean toBeUpdated=false;
    private boolean toBeDeleted=false;
    private boolean inActiveUse = true;

    @Ignore
    private static final String TAG = TrashPlayConstants.TAG;

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public boolean isToBeDeleted() {
        return toBeDeleted;
    }

    public void setToBeDeleted(boolean toBeDeleted) {
        this.toBeDeleted = toBeDeleted;
    }

    public boolean isToBeUpdated() {
        return toBeUpdated;
    }

    public void setToBeUpdated(boolean toBeUpdated) {
        this.toBeUpdated = toBeUpdated;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public void setDurationInSeconds(int durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public int getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setPlayLists(RealmList<PlayList> list) {
        playLists = list;
    }

    public RealmList<PlayList> getPlayLists() {
        return playLists;
    }


    public void setPlays(int p) {
        plays = p;
    }

    public int getPlays() {
        return plays;
    }


    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isInActiveUse() {
        return inActiveUse;
    }

    public void setInActiveUse(boolean inActiveUse) {
        this.inActiveUse = inActiveUse;
    }
}
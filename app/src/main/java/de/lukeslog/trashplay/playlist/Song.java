package de.lukeslog.trashplay.playlist;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.ArrayList;

import de.lukeslog.trashplay.constants.TrashPlayConstants;

@Table(name = "Songs")
public class Song extends Model {

    @Column(name = "fileName")
    private String fileName="";

    @Column(name = "playLists")
    private String playLists;

    @Column(name = "songName")
    private String songName="";

    @Column(name = "artist")
    private String artist="";

    @Column(name = "lastUpdate")
    private long lastUpdate=0l;

    @Column(name = "lastPlayed")
    private long lastPlayed =0l;

    @Column(name = "durationInSeconds")
    private int durationInSeconds = 0;

    @Column(name = "plays")
    private int plays = 0;

    @Column(name = "reasonForRepl")
    private String reasonForReplacementInNextPlays = "";

    @Column(name = "toBeUpdated")
    private boolean toBeUpdated=false;

    @Column(name = "toBeDeleted")
    private boolean toBeDeleted=false;

    @Column(name = "inActiveUse")
    private boolean inActiveUse = true;

    public Song() {

    }

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

    public void setPlayLists(String list) {
        playLists = list;
    }

    public String getPlayLists() {
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

    public long getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getReasonForReplacementInNextPlays() {
        return reasonForReplacementInNextPlays;
    }

    public void setReasonForReplacementInNextPlays(String reasonForReplacementInNextPlays) {
        this.reasonForReplacementInNextPlays = reasonForReplacementInNextPlays;
    }


}
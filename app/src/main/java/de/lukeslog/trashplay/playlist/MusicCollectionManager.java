package de.lukeslog.trashplay.playlist;

import android.content.SharedPreferences;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.cloudstorage.DropBox;
import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.SettingsConstants;

/**
 * This class is a singleton which is in charge of managing the access of the music player to the
 * music. No class handles PlayLists or Songs directly, everything goes through this class
 */
public class MusicCollectionManager {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    private static final int LENGTH_OF_PLAYLIST = 10;

    private static MusicCollectionManager instance = null;

    private int numberOfActivatedPlayLists = 0;

    private long startTimeOfCurrentSong = 0l;

    public static String radiofile;
    private static boolean radioFileHasChanged = true;

    private ArrayList<Long> timeStampsForRadio = new ArrayList<Long>();

    public static long timeDifInMillis = 0l;
    public static long lastStartTime = 0l;

    private ArrayList<Song> nextSongs = new ArrayList<Song>();

    private MusicCollectionManager() {
    }

    public static synchronized MusicCollectionManager getInstance() {
        if (instance == null) {
            instance = new MusicCollectionManager();
        }
        return instance;
    }

    public void finishedSong() {
        Logger.d(TAG, "finished Song");
        if (!nextSongs.isEmpty()) {
            Logger.d(TAG, "remove 0");
            Song s = nextSongs.remove(0);
            try {
                SongHelper.finishedWithSong(s.getFileName());
            } catch (Exception e) {
                Logger.e(TAG, "finishedSong in MusicCollectionManager");
            }
        }

    }

    public Song getNextSong() throws Exception {
        Logger.d(TAG, "get Next Song");
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        boolean listenalong = settings.getBoolean("listenalong", false);
        if (listenalong) {
            if (radioFileHasChanged) {
                parseRadioFile();
            }
            Logger.d(TAG, "Next Song for Radio People...");
            Logger.d(TAG, "continue");
            DateTime now = new DateTime();
            Logger.d(TAG, "NOW: " + now);
            if (timeStampsForRadio != null && timeStampsForRadio.get(0) != null) {
                if (timeStampsForRadio.size() > 1 && timeStampsForRadio.get(0) == lastStartTime) {
                    timeStampsForRadio.remove(0);
                    nextSongs.remove(0);
                }
                DateTime then = new DateTime(timeStampsForRadio.get(0));
                Logger.d(TAG, "PLAY TIME:" + then);
                timeDifInMillis = now.getMillis() - then.getMillis();
                lastStartTime = timeStampsForRadio.get(0);
                Log.d(TAG, "" + (timeDifInMillis / 1000));
            }
            Song song = null;
            if (nextSongs != null) {
                song = nextSongs.get(0);
                if (song != null) {
                    Log.d(TAG, "------------->" + song.getFileName());
                }
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            getRadioStationFile();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                if (radioFileHasChanged) {
                    parseRadioFile();
                    Log.d(TAG, "back on top...");
                }
                Log.d(TAG, "--------------");
                if (song != null) {
                    Log.d(TAG, "--------------SONG NOT NULL:::");
                    return song;
                }
            }
        }
        timeDifInMillis = 0l;
        refreshPlayList();
        if (!nextSongs.isEmpty()) {

            Song song = nextSongs.get(0);
            if (song != null) {
                Logger.d(TAG, "next song: " + SongHelper.getTitleInfoAsString(song));
            } else {
                Logger.d(TAG, "SONG IS NULL! THIS IS FUCKED UP");
            }
            startTimeOfCurrentSong = new DateTime().getMillis();
            updateRadioFile();
            return song;
        }
        return null;
    }

    private void parseRadioFile() {
        Logger.d(TAG, "parseradiofile....");

        if (nextSongs.size() < 10) {
            for (int i = nextSongs.size(); i < 10; i++) {
                nextSongs.add(null);
            }
        }
        if (timeStampsForRadio.size() < 10) {
            Logger.d(TAG, "S2 " + timeStampsForRadio.size());
            for (int i = timeStampsForRadio.size(); i < 10; i++) {
                timeStampsForRadio.add(null);
            }
        }
        if (radiofile != null) {
            String[] lines = radiofile.split("\n");
            Logger.d(TAG, "lines " + lines.length);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String[] parts = line.split(" ");
                String fname = "";
                for (int j = 0; j < parts.length - 1; j++) {
                    fname = fname + parts[j] + " ";
                }
                fname = fname.trim();
                String timestampstring = parts[parts.length - 1];
                Logger.d(TAG, fname);
                Logger.d(TAG, timestampstring);
                int subtractor = 0;
                if (i < 10) {
                    Logger.d(TAG, "->");
                    Song s = SongHelper.getSongByFileName(fname);
                    Logger.d(TAG, "-->");
                    nextSongs.set(i - subtractor, s);
                    Logger.d(TAG, "--->");
                    timeStampsForRadio.set(i - subtractor, Long.parseLong(timestampstring));
                    Logger.d(TAG, "x");
                    if (s == null) {
                        subtractor++;
                    }
                }
            }
            radioFileHasChanged = false;
        }
        Logger.d(TAG, "done...");
    }

    public void getRadioStationFile() throws Exception {
        Logger.d(TAG, "getRadioStationFile");
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        String radioStation = settings.getString("radiostation", "");
        Logger.d(TAG, "radiostation" + radioStation);
        if (!radioStation.equals("")) {
            Logger.d(TAG, "SPLIT");
            String[] stationdata = radioStation.split("_");
            Logger.d(TAG, "->" + stationdata.length);
            Logger.d(TAG, stationdata[1]);
            StorageManager remoteStorage = StorageManager.getStorage(stationdata[1]);
            Log.d(TAG, "--x--");
            String playlistfile = remoteStorage.downloadFile(stationdata[2] + "/Radio", stationdata[3]);
            Logger.d(TAG, StorageManager.LOCAL_STORAGE + playlistfile);
            radiofile = getStringFromFile(StorageManager.LOCAL_STORAGE + playlistfile);
            radioFileHasChanged = true;
        }
    }

    public void updateRadioFile() throws Exception {
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        boolean radioMode = settings.getBoolean(SettingsConstants.APP_SETTING_RADIO_MODE, false);
        if (radioMode) {
            Logger.d(TAG, "Radiomode... update Next Songs To DB");
            String nextSongsinRadioWithTimeStamps = "";
            long nowlong = startTimeOfCurrentSong;
            for (Song song : nextSongs) {
                nextSongsinRadioWithTimeStamps = nextSongsinRadioWithTimeStamps + song.getFileName() + " " + nowlong + "\n";
                nowlong = nowlong + song.getDurationInSeconds();
            }
            Logger.d(TAG, nextSongsinRadioWithTimeStamps);
            String oldRadioString = settings.getString("radioSongs", "");
            if (!nextSongsinRadioWithTimeStamps.equals(oldRadioString)) {
                Logger.d(TAG, "ololroflcopter");
                SharedPreferences.Editor edit = settings.edit();
                edit.putString("radioSongs", nextSongsinRadioWithTimeStamps);
                edit.commit();
                Logger.d(TAG, "....");
                List<PlayList> allplaylists = PlayListHelper.getAllPlayLists();
                for (final PlayList playList : allplaylists) {
                    Logger.d(TAG, playList.getRemotePath());
                    if (playList.isActivated()) {
                        Logger.d(TAG, "found active playlist...");
                        final String path = playList.getRemotePath();
                        Logger.d(TAG, playList.getRemotePath());
                        final StorageManager storage = StorageManager.getStorage(playList.getRemoteStorage());
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    storage.updateRadioFile(path);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
            } else {
                Logger.d(TAG, "nochange...");
            }

        }
    }

    public ArrayList<Song> getListOfNextSongs() {
        if (TrashPlayService.serviceRunning()) {
            if (nextSongs.isEmpty()) {
                recoverNextSongsFromSettings();
            }
        }
        return nextSongs;
    }

    private void refreshPlayList() {
        if (nextSongs.isEmpty()) {
            recoverNextSongsFromSettings();
        }
        Logger.d(TAG, "refresh playlist");
        for (int i = 0; i < nextSongs.size(); i++) {
            Logger.d(TAG, "refresh");
            Song s = replaceSongForReasons(nextSongs.get(i));
            if (s != null) {
                setNextSongPlayListInSettings(i, s.getFileName());
                nextSongs.set(i, s);
            }
        }
        nextSongs.remove(null);
        if (nextSongs.size() < LENGTH_OF_PLAYLIST) {
            int numberOfSongsToAdd = LENGTH_OF_PLAYLIST - nextSongs.size();
            Logger.d(TAG, "refresh has to add " + numberOfSongsToAdd + " songs");
            for (int i = 0; i < numberOfSongsToAdd; i++) {
                Song song = pickASong();
                Logger.d(TAG, "->" + i + " " + SongHelper.getTitleInfoAsString(song));
                setNextSongPlayListInSettings(nextSongs.size(), song.getFileName());
                nextSongs.add(song);
            }
        }
        Logger.d(TAG, "done with playlist refreshing");
    }

    private void setNextSongPlayListInSettings(int i, String fileName) {
        Logger.d(TAG, "put into settings");
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        SharedPreferences.Editor edit = settings.edit();
        edit.putString("nextSong_" + i, fileName);
        edit.commit();
    }

    private void recoverNextSongsFromSettings() {
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        if (settings != null) {
            for (int i = 0; i < 10; i++) {
                String nextSongFileName = settings.getString("nextSong_" + i, "");
                if (nextSongFileName.equals("")) {
                    break;
                }
                nextSongs.add(SongHelper.getSongByFileName(nextSongFileName));
            }
        }

    }

    public Song pickASong() {
        Logger.d(TAG, "pickASong()");
        SongSelector songSelector = SongSelectorFactory.getSongSelector(SongSelector.EQUAL_PLAY_SONG_SELECTOR);
        Song possibleSong = songSelector.getASong();
        return replaceSongForReasons(possibleSong);
    }

    private Song replaceSongForReasons(Song possibleSong) {
        Logger.d(TAG, "replaceForReasons...");
        Logger.d(TAG, "check if song needs to be replaced (" + SongHelper.getTitleInfoAsString(possibleSong) + ")");
        if (SongHelper.getNumberOfViableSongs() > 0 && getNumberOfActivePlayLists() > 0) {
            Logger.d(TAG, "number if viable songs is more than 0, thats good");
            if (possibleSong == null) {
                SongHelper.determineNumberOfViableSongs();
                return pickASong();
            }
            try {
                possibleSong.getFileName(); //if this throws a database exception.,.
            } catch (ArrayIndexOutOfBoundsException e) {
                Logger.d(TAG, "this song throws an index out of bounds exception...");
                return pickASong();
            }
            if (possibleSong.isToBeDeleted()) {
                Logger.d(TAG, "isToBeDeleted");
                return pickASong();
            }
            if (possibleSong.isToBeUpdated()) {
                Logger.d(TAG, "isToBeUpdated");
                return pickASong();
            }
            if (!SongHelper.localFileExists(possibleSong)) {
                Logger.d(TAG, "localFiledoesNotExist");
                return pickASong();
            }
            if (!possibleSong.isInActiveUse()) {
                return pickASong();
            }
            return possibleSong;
            //TODO: check if the user wants to avoid repeating songs, replace song if its the not the next song but equals to the next song
        }
        return null;
    }

    public void syncRemoteStorageWithDevice(final boolean lookForNewPlayLists) {
        if (TrashPlayService.wifi) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        if (lookForNewPlayLists || MusicCollectionManager.getInstance().getNumberOfActivePlayLists() == 0) {
                            findRemotePlayListFoldersAndCreateNewPlayLists();
                        }
                        synchronizePlayLists();
                    } catch (Exception e) {
                        Logger.e(TAG, "Exception while trying to sync.");
                        syncRemoteStorageWithDevice(lookForNewPlayLists);
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void synchronizePlayLists() throws Exception {
        Logger.d(TAG, "Synchronize");
        List<PlayList> allPlayLists = PlayListHelper.getAllPlayLists();
        Logger.d(TAG, "getAll");
        for (PlayList playlist : allPlayLists) {
            PlayListHelper.synchronize(playlist);
            SongHelper.determineNumberOfViableSongs();
        }
        Logger.d(TAG, "ok.");
    }

    private void findRemotePlayListFoldersAndCreateNewPlayLists() throws Exception {
        Logger.d(TAG, "findRemotePlayListFoldersAndCreateNewPlayLists()");
        if (DropBox.getInstance().isConnected()) {

            DropBox newDropBox = DropBox.getInstance();

            List<String> folderNames = newDropBox.getAllPlayListFolders();
            Logger.d(TAG, "Done with the search");
            Logger.d(TAG, "returned with a list of " + folderNames.size());
            for (String folderName : folderNames) {
                Logger.d(TAG, folderName);
                if (!knownPlaylist(folderName)) {
                    if (!TrashPlayService.getContext().isInRadioMode()) {
                        Logger.d(TAG, "create New PlayList since we are not in radiomode");
                        PlayListHelper.createNewPlayList(newDropBox, folderName);
                    }
                    else {
                        Logger.d(TAG, "a new playlist was found but not downloaded since we are in radiomode");
                        TrashPlayService.getContext().toast("a new playlist was found but not downloaded since we are in radiomode");
                    }
                }
            }
        }
    }

    private boolean knownPlaylist(String folderName) {

        List<PlayList> allPlayLists = PlayListHelper.getAllPlayLists();
        for (PlayList playList : allPlayLists) {
            if (playList.getRemotePath().equals(folderName)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> getListOfAllowedFileEndings() {
        List<String> fileEndings = new ArrayList<String>();
        fileEndings.add("mp3");
        return fileEndings;
    }

    public List<Song> getSongsByPlayList(PlayList playList) {
        List<Song> songsInPlayList = new ArrayList<Song>();
        List<Song> allSongs = SongHelper.getAllSongs();
        for (Song song : allSongs) {
            if (SongHelper.isInPlayList(song, playList)) {
                songsInPlayList.add(song);
            }
        }
        return songsInPlayList;
    }

    public boolean collectionNotEmpty() {
        return !(SongHelper.getNumberOfViableSongs() == 0);
    }

    public int getNumberOfActivePlayLists() {
        return numberOfActivatedPlayLists;
    }

    public void determineNumberOfActivatedPlayLists() {
        Logger.d(TAG, "getNumberOFActivatedPlayLIsts");
        int n = 0;
        List<PlayList> allplayLists = PlayListHelper.getAllPlayLists();
        for (PlayList playList : allplayLists) {
            if (playList.isActivated()) {
                n++;
            }
        }
        Logger.d(TAG, "->" + n);
        numberOfActivatedPlayLists = n;
    }

    public String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public void resetNextSongs() {
        nextSongs.clear();
    }
}
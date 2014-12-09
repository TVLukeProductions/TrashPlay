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

    private int numberOfViableSongs = 0;
    private int numberOfActivatedPlayLists = 0;

    private long startTimeOfCurrentSong = 0l;

    public static String radiofile;
    private static boolean radioFileHasChanged = true;

    private ArrayList<Long> timeStampsForRadio = new ArrayList<Long>();

    public static long timeDifInMillis = 0l;

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
        Log.d(TAG, "finished Song");
        if (!nextSongs.isEmpty()) {
            Log.d(TAG, "remove 0");
            Song s = nextSongs.remove(0);
            try {
                SongHelper.finishedWithSong(s.getFileName());
            } catch (Exception e) {
                Log.e(TAG, "finishedSong in MusicCollectionManager");
            }
        }

    }

    public Song getNextSong() throws Exception {
        Log.d(TAG, "get Next Song");
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        boolean listenalong = settings.getBoolean("listenalong", false);
        if (listenalong) {
            if (radiofile == null) {
                parseRadioFile();
            }
            Log.d(TAG, "Next Song for Radio People...");
            Log.d(TAG, "continue");
            DateTime now = new DateTime();
            Log.d(TAG, "NOW: " + now);
            DateTime then = new DateTime(timeStampsForRadio.get(0));
            Log.d(TAG, "PLAY TIME:" + then);
            timeDifInMillis = now.getMillis() - then.getMillis();
            Log.d(TAG, "" + (timeDifInMillis / 1000));
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
                Log.d(TAG, "next song: " + SongHelper.getTitleInfoAsString(song));
            } else {
                Log.d(TAG, "SONG IS NULL! THIS IS FUCKED UP");
            }
            startTimeOfCurrentSong = new DateTime().getMillis();
            updateRadioFile();
            return song;
        }
        return null;
    }

    private void parseRadioFile() {
        if (nextSongs.size() < 10) {
            for (int i = nextSongs.size(); i < 10; i++) {
                nextSongs.add(null);
            }
        }
        if (timeStampsForRadio.size() < 10) {
            Log.d(TAG, "S2 " + timeStampsForRadio.size());
            for (int i = timeStampsForRadio.size(); i < 10; i++) {
                timeStampsForRadio.add(null);
            }
        }
        if (radiofile != null) {
            String[] lines = radiofile.split("\n");
            Log.d(TAG, "lines " + lines.length);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String[] parts = line.split(" ");
                String fname = "";
                for (int j = 0; j < parts.length - 1; j++) {
                    fname = fname + parts[j] + " ";
                }
                fname = fname.trim();
                String timestampstring = parts[parts.length - 1];
                Log.d(TAG, fname);
                Log.d(TAG, timestampstring);
                int subtractor = 0;
                if (i < 10) {
                    Log.d(TAG, "->");
                    Song s = SongHelper.getSongByFileName(fname);
                    Log.d(TAG, "-->");
                    nextSongs.set(i - subtractor, s);
                    Log.d(TAG, "--->");
                    timeStampsForRadio.set(i - subtractor, Long.parseLong(timestampstring));
                    Log.d(TAG, "x");
                    if (s == null) {
                        subtractor++;
                    }
                }
            }
            radioFileHasChanged = false;
        }
        Log.d(TAG, "done...");
    }

    public void getRadioStationFile() throws Exception {
        Log.d(TAG, "getRadioStationFile");
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        String radioStation = settings.getString("radiostation", "");
        Log.d(TAG, "radiostation" + radioStation);
        if (!radioStation.equals("")) {
            Log.d(TAG, "SPLIT");
            String[] stationdata = radioStation.split("_");
            Log.d(TAG, "->" + stationdata.length);
            Log.d(TAG, stationdata[1]);
            StorageManager remoteStorage = StorageManager.getStorage(stationdata[1]);
            Log.d(TAG, "--x--");
            String playlistfile = remoteStorage.downloadFile(stationdata[2] + "/Radio", stationdata[3]);
            Log.d(TAG, StorageManager.LOCAL_STORAGE + playlistfile);
            radiofile = getStringFromFile(StorageManager.LOCAL_STORAGE + playlistfile);
            radioFileHasChanged = true;
        }
    }

    public void updateRadioFile() throws Exception {
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        boolean radioMode = settings.getBoolean(SettingsConstants.APP_SETTING_RADIO_MODE, false);
        if (radioMode) {
            Log.d(TAG, "Radiomode... update Next Songs To DB");
            String nextSongsinRadioWithTimeStamps = "";
            long nowlong = startTimeOfCurrentSong;
            for (Song song : nextSongs) {
                nextSongsinRadioWithTimeStamps = nextSongsinRadioWithTimeStamps + song.getFileName() + " " + nowlong + "\n";
                nowlong = nowlong + song.getDurationInSeconds();
            }
            Log.d(TAG, nextSongsinRadioWithTimeStamps);
            String oldRadioString = settings.getString("radioSongs", "");
            if (!nextSongsinRadioWithTimeStamps.equals(oldRadioString)) {
                Log.d(TAG, "ololroflcopter");
                SharedPreferences.Editor edit = settings.edit();
                edit.putString("radioSongs", nextSongsinRadioWithTimeStamps);
                edit.commit();
                Log.d(TAG, "....");
                List<PlayList> allplaylists = PlayListHelper.getAllPlayLists();
                for (final PlayList playList : allplaylists) {
                    Log.d(TAG, playList.getRemotePath());
                    if (playList.isActivated()) {
                        Log.d(TAG, "found active playlist...");
                        final String path = playList.getRemotePath();
                        Log.d(TAG, playList.getRemotePath());
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
                Log.d(TAG, "nochange...");
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
        Log.d(TAG, "refresh playlist");
        for (int i = 0; i < nextSongs.size(); i++) {
            Log.d(TAG, "refresh");
            Song s = replaceSongForReasons(nextSongs.get(i));
            if (s != null) {
                setNextSongPlayListInSettings(i, s.getFileName());
                nextSongs.set(i, s);
            }
        }
        nextSongs.remove(null);
        if (nextSongs.size() < LENGTH_OF_PLAYLIST) {
            int numberOfSongsToAdd = LENGTH_OF_PLAYLIST - nextSongs.size();
            Log.d(TAG, "refresh has to add " + numberOfSongsToAdd + " songs");
            for (int i = 0; i < numberOfSongsToAdd; i++) {
                Song song = pickASong();
                Log.d(TAG, "->" + i + " " + SongHelper.getTitleInfoAsString(song));
                setNextSongPlayListInSettings(nextSongs.size(), song.getFileName());
                nextSongs.add(song);
            }
        }
        Log.d(TAG, "done with playlist refreshing");
    }

    private void setNextSongPlayListInSettings(int i, String fileName) {
        Log.d(TAG, "put into settings");
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
        Log.d(TAG, "pickASong()");
        SongSelector songSelector = SongSelectorFactory.getSongSelector(SongSelector.EQUAL_PLAY_SONG_SELECTOR);
        Song possibleSong = songSelector.getASong();
        return replaceSongForReasons(possibleSong);
    }

    private Song replaceSongForReasons(Song possibleSong) {
        Log.d(TAG, "replaceForReasons...");
        Log.d(TAG, "check if song needs to be replaced (" + SongHelper.getTitleInfoAsString(possibleSong) + ")");
        if (getNumberOfViableSongs() > 0 && getNumberOfActivePlayLists() > 0) {
            Log.d(TAG, "number if viable songs is more than 0, thats good");
            if (possibleSong == null) {
                determineNumberOfViableSongs();
                return pickASong();
            }
            try {
                possibleSong.getFileName(); //if this throws a database exception.,.
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.d(TAG, "this song throws an index out of bounds exception...");
                return pickASong();
            }
            if (possibleSong.isToBeDeleted()) {
                Log.d(TAG, "isToBeDeleted");
                return pickASong();
            }
            if (possibleSong.isToBeUpdated()) {
                Log.d(TAG, "isToBeUpdated");
                return pickASong();
            }
            if (!SongHelper.localFileExists(possibleSong)) {
                Log.d(TAG, "localFiledoesNotExist");
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

    public void syncRemoteStorageWithDevice(final boolean lookForNewPlayLists) throws Exception {
        if (TrashPlayService.wifi) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        if (lookForNewPlayLists || MusicCollectionManager.getInstance().getNumberOfActivePlayLists() == 0) {
                            findRemotePlayListFoldersAndCreateNewPlayLists();
                        }
                        synchronizePlayLists();
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while trying to sync.");
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void synchronizePlayLists() throws Exception {
        Log.d(TAG, "Synchronize");
        List<PlayList> allPlayLists = PlayListHelper.getAllPlayLists();
        Log.d(TAG, "getAll");
        for (PlayList playlist : allPlayLists) {
            PlayListHelper.synchronize(playlist);
            determineNumberOfViableSongs();
        }
        Log.d(TAG, "ok.");
    }

    private void findRemotePlayListFoldersAndCreateNewPlayLists() throws Exception {
        if (DropBox.getInstance().isConnected()) {

            DropBox newDropBox = DropBox.getInstance();

            List<String> folderNames = newDropBox.getAllPlayListFolders();
            Log.d(TAG, "Done with the search");
            Log.d(TAG, "returned with a list of " + folderNames.size());
            for (String folderName : folderNames) {
                Log.d(TAG, folderName);
                if (!knownPlaylist(folderName)) {
                    PlayListHelper.createNewPlayList(newDropBox, folderName);
                }
            }
        }
    }

    private boolean knownPlaylist(String foldername) {

        List<PlayList> allPlayLists = PlayListHelper.getAllPlayLists();
        for (PlayList playList : allPlayLists) {
            if (playList.getRemotePath().equals(foldername)) {
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
        SongHelper.removeSongsThatAreToBeDeleted();
        return !(getNumberOfViableSongs() == 0);
    }

    public void determineNumberOfViableSongs() {
        List<Song> songs = SongHelper.getAllSongs();
        int n = 0;
        for (Song s : songs) {
            if (!s.isToBeDeleted() && !s.isToBeUpdated() && s.isInActiveUse()) {
                n++;
            }
        }
        numberOfViableSongs = n;
    }

    public int getNumberOfViableSongs() {
        return numberOfViableSongs;
    }

    public int getNumberOfActivePlayLists() {
        return numberOfActivatedPlayLists;
    }

    public void determineNumberOfActivatedPlayLists() {
        Log.d(TAG, "getNumberOFActivatedPlayLIsts");
        int n = 0;
        List<PlayList> allplayLists = PlayListHelper.getAllPlayLists();
        for (PlayList playList : allplayLists) {
            if (playList.isActivated()) {
                n++;
            }
        }
        Log.d(TAG, "->" + n);
        numberOfActivatedPlayLists = n;
    }

    public String getStringFromFile(String filePath) throws Exception {
        Log.d(TAG, "-");
        File fl = new File(filePath);
        Log.d(TAG, "-");
        FileInputStream fin = new FileInputStream(fl);
        Log.d(TAG, "-");
        String ret = convertStreamToString(fin);
        Log.d(TAG, "-");
        //Make sure you close all streams.
        fin.close();
        Log.d(TAG, "-");
        return ret;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        Log.d(TAG, "--");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Log.d(TAG, "--");
        StringBuilder sb = new StringBuilder();
        Log.d(TAG, "--");
        String line = null;
        while ((line = reader.readLine()) != null) {
            Log.d(TAG, "---");
            sb.append(line).append("\n");
        }
        Log.d(TAG, "--");
        reader.close();
        Log.d(TAG, "--");
        return sb.toString();
    }
}
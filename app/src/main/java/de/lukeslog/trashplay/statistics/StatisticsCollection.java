package de.lukeslog.trashplay.statistics;

import android.content.SharedPreferences;
import android.location.Location;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.player.MusicPlayer;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.playlist.SongHelper;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;

public class StatisticsCollection {

    public static final String TAG = TrashPlayConstants.TAG;

    private static String TRASH_PLAY_PLAY_TIME = "trashplayplaytime";
    private static String RADIO_PLAY_PLAY_TIME = "trashplayplaytime";
    private static String PLAY_TIME = "allplaytime";

    private static String DIF_AMOUNT_COLLECTED = "amountofvaluesfordif";
    private static String DIF_BETWEEN_PLAYS = "difbetweenplays";

    private static String TOTAL_DISTANCE = "totalDistance";

    private static String SONGS_PER_DAY = "songsperday";

    private static String LONGEST_STREAK_SONG_NAME = "longestStreakSongName";
    private static String LONGEST_STREAK_LENGTH = "longestStreakLength";
    private static String STREAK = "Streak";

    private static String ACTIVE_SINCE = "activeSince";

    private static DateTime lastPingTrashPlay;
    private static DateTime lastPingRadioPlay;
    private static DateTime lastPing = null;
    private static int plays = 0;

    private static String fileNameOfLastSong = "";
    private static int currentStreakLength = 0;
    private static long lastTimeLocationChanged = 0l;
    private static float currentSpeed = 0;

    private static Location location = null;

    public static void ping() {
        SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
        boolean trashMode = TrashPlayService.getContext().isInTrashMode();
        boolean radioMode = TrashPlayService.getContext().isInRadioMode();
        boolean listenalong = settings.getBoolean("listenalong", false);
        boolean playing = MusicPlayer.getCurrentlyPlayingSong() != null;
        long activeSince = settings.getLong(ACTIVE_SINCE, 0l);
        if (activeSince == 0l) {
            activeSince = new DateTime().getMillis();
            SharedPreferences.Editor edit = settings.edit();
            edit.putLong(ACTIVE_SINCE, activeSince);
            edit.commit();
        }

        if (playing) {
            DateTime now = new DateTime();
            if (trashMode) {
                addToTrashPlayPlayTime(now);
            }
            if (radioMode) {
                addToRadioPlayTime(now);
            }
            if (listenalong) {

            }
            addToPlayTime(now);
        }
    }

    private static void addToRadioPlayTime(DateTime now) {
        if (lastPingRadioPlay == null) {
            lastPingRadioPlay = now;
        }
        long before = lastPingRadioPlay.getMillis();
        long later = now.getMillis();
        if (later - before < 6500) {
            SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
            long tppt = settings.getLong(RADIO_PLAY_PLAY_TIME, 0l);
            long tppt_perday = settings.getLong(RADIO_PLAY_PLAY_TIME + "_" + now.getYear() + "_" + now.getMonthOfYear() + "_" + now.getDayOfMonth(), 0l);
            tppt = tppt + (later - before);
            tppt_perday = tppt_perday + (later - before);
            SharedPreferences.Editor edit = settings.edit();
            edit.putLong(RADIO_PLAY_PLAY_TIME, tppt);
            edit.putLong(RADIO_PLAY_PLAY_TIME + "_" + now.getYear() + "_" + now.getMonthOfYear() + "_" + now.getDayOfMonth(), tppt_perday);
            edit.commit();
        }
        lastPingRadioPlay = now;
    }

    private static void addToPlayTime(DateTime now) {
        if (lastPing == null) {
            lastPing = now;
        }
        long before = lastPing.getMillis();
        long later = now.getMillis();
        if (later - before < 6500) {
            SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
            long tppt = settings.getLong(PLAY_TIME, 0l);
            tppt = tppt + (later - before);
            SharedPreferences.Editor edit = settings.edit();
            edit.putLong(PLAY_TIME, tppt);
            edit.commit();
        }
        lastPing = now;
    }

    private static void addToTrashPlayPlayTime(DateTime now) {
        if (lastPingTrashPlay == null) {
            lastPingTrashPlay = now;
        }
        long before = lastPingTrashPlay.getMillis();
        long later = now.getMillis();
        if (later - before < 6500) {
            SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
            long tppt = settings.getLong(TRASH_PLAY_PLAY_TIME, 0l);
            tppt = tppt + (later - before);
            SharedPreferences.Editor edit = settings.edit();
            edit.putLong(TRASH_PLAY_PLAY_TIME, tppt);
            edit.commit();
        }
        lastPingTrashPlay = now;
    }

    public static long getPlayTime() {
        SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
        long tppt = settings.getLong(PLAY_TIME, 0l);
        return tppt;
    }

    public static void finishedSong(String songName, long timeDifBetweenPlays) {
        DateTime now = new DateTime();

        SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
        float dif = settings.getFloat(DIF_BETWEEN_PLAYS, 0f);
        float difamount = settings.getFloat(DIF_AMOUNT_COLLECTED, 0f);

        float dif_s = settings.getFloat(DIF_BETWEEN_PLAYS + "_" + songName, 0f);
        float difamount_s = settings.getFloat(DIF_AMOUNT_COLLECTED + "_" + songName, 0f);

        int songsperday = settings.getInt(SONGS_PER_DAY + "_" + now.getYear() + "_" + now.getMonthOfYear() + "_" + now.getDayOfMonth(), 0);

        float d = dif * difamount;
        d = d + timeDifBetweenPlays;
        d = d / (difamount + 1);

        float d_s = dif_s * difamount_s;
        d_s = d_s + timeDifBetweenPlays;
        d_s = d_s / (difamount_s + 1);

        SharedPreferences.Editor edit = settings.edit();
        edit.putFloat(DIF_BETWEEN_PLAYS, d);
        edit.putFloat(DIF_AMOUNT_COLLECTED, (difamount + 1));

        edit.putFloat(DIF_BETWEEN_PLAYS + "_" + songName, d_s);
        edit.putFloat(DIF_AMOUNT_COLLECTED + "_" + songName, (difamount_s + 1));

        edit.putInt(SONGS_PER_DAY + "_" + now.getYear() + "_" + now.getMonthOfYear() + "_" + now.getDayOfMonth(), (songsperday + 1));
        edit.commit();

        //song streaks
        determineStreak(songName);
    }

    private static void determineStreak(String songName) {
        SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
        int longest = settings.getInt(LONGEST_STREAK_LENGTH, 0);
        if (fileNameOfLastSong.equals(songName)) {
            currentStreakLength++;
        } else {
            currentStreakLength = 1;
        }
        if (currentStreakLength > longest && currentStreakLength>1) {
            SharedPreferences.Editor edit = settings.edit();
            edit.putInt(LONGEST_STREAK_LENGTH, currentStreakLength);
            edit.putString(LONGEST_STREAK_SONG_NAME, songName);
            edit.commit();
        }
        int longestForSong = settings.getInt(STREAK + "_" + songName, 0);
        if (currentStreakLength > longestForSong) {
            SharedPreferences.Editor edit = settings.edit();
            edit.putInt(STREAK + "_" + songName, currentStreakLength);
            edit.commit();
        }
        fileNameOfLastSong = songName;
    }


    public static int getPlays() {
        List<Song> songs = SongHelper.getAllSongsOrderedByPlays();
        plays = 0;
        for (Song song : songs) {
            if (song.getPlays() > 0) {
                plays = plays + song.getPlays();
            } else {
                break;
            }
        }
        return plays;
    }

    public static Integer[] playsFromTo(DateTime start, DateTime end) {
        start = start.withTime(0, 0, 0, 0);
        end = end.withTime(0, 0, 0, 0);
        end = end.plusDays(1).minusMillis(1);
        ArrayList<Integer> playsPerDay = new ArrayList<Integer>();
        while (start.isBefore(end)) {
            SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
            playsPerDay.add(settings.getInt(SONGS_PER_DAY + "_" + start.getYear() + "_" + start.getMonthOfYear() + "_" + start.getDayOfMonth(), 0));
            start = start.plusDays(1);
        }
        Integer[] result = new Integer[playsPerDay.size()];
        result = playsPerDay.toArray(result);
        return result;
    }

    public static void newLocation(Location newLocation) {

        if (location == null) {
            location = newLocation;
        } else {
            if (newLocation.getAccuracy() < 50 && location.distanceTo(newLocation) > 1000) {
                addToDistance(newLocation);
            }
        }
    }

    private static void addToDistance(Location newLocation) {
        DateTime now = new DateTime();
        float distanceInMeters = location.distanceTo(newLocation);
        SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
        boolean trashMode = TrashPlayService.getContext().isInTrashMode();
        boolean listenalong = settings.getBoolean("listenalong", false);
        boolean playing = MusicPlayer.getCurrentlyPlayingSong() != null;
        if (playing && (trashMode || listenalong)) {
            float totalDistance = settings.getFloat(TOTAL_DISTANCE, 0l);
            totalDistance = totalDistance + distanceInMeters;
            SharedPreferences.Editor edit = settings.edit();
            edit.putFloat(TOTAL_DISTANCE, totalDistance);
            edit.commit();
        }
        if (lastTimeLocationChanged > 0l) {
            float timedif = now.getMillis() - lastTimeLocationChanged;
            float timeDifInH = (((timedif / 1000) / 60) / 60);
            float kmh = (distanceInMeters / 1000) / timeDifInH;
            currentSpeed = kmh;
        }
        lastTimeLocationChanged = now.getMillis();
        location = newLocation;
    }

    public static float getTotalDistance() {
        SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
        return settings.getFloat(TOTAL_DISTANCE, 0l);
    }

    public static int getLongestStreak() {
        SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
        return settings.getInt(LONGEST_STREAK_LENGTH, 0);
    }

    public static String getLongestStreakSongName() {
        SharedPreferences settings = TrashPlayService.getContext().getDefaultSettings();
        return settings.getString(LONGEST_STREAK_SONG_NAME, "");
    }

    public static float getCurrentSpeed() {
        return currentSpeed;
    }
}

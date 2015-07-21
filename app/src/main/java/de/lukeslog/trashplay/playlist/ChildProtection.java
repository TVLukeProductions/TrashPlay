package de.lukeslog.trashplay.playlist;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;

public class ChildProtection {

    public static final String TAG = TrashPlayConstants.TAG;

    private static List<String> blackListArtist = new ArrayList<String>();

    private static List<String> blackListFiles = new ArrayList<String>();

    public static boolean isSongOk(Song possibleSong) {

        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        Boolean childProtectionActivated = settings.getBoolean("pref_childprotect", false);
        if(!childProtectionActivated) {
            return true;
        }
        updateLists();
        if(blackListArtist.contains(possibleSong.getArtist())){
            Logger.e(TAG, "removed file "+possibleSong.getFileName());
            return false;
        }
        if(blackListFiles.contains(possibleSong.getFileName())) {
            Logger.e(TAG, "removed file "+possibleSong.getFileName());
            return false;
        }
        return true;
    }

    //TODO: long term this should probably be part of the dropbox... but thats anoying.
    private static void updateLists() {
        blackListFiles.add("Massiv - Ghettolied (Beathoavenz Remix).mp3");
        blackListFiles.add("Frank Styles - Die mit dem Roten Halsband.mp3");
        blackListArtist.add("K.I.Z.");
    }
}

package de.lukeslog.trashplay.radio;

import android.content.SharedPreferences;

import de.lukeslog.trashplay.service.TrashPlayService;

public class RadioManager {

    public static void setRadioMode(boolean mode) {
        if(TrashPlayService.serviceRunning()) {
            SharedPreferences.Editor edit = TrashPlayService.getDefaultSettings().edit();
            edit.putBoolean("listenalong", mode);
            edit.commit();
        }
    }
}

package de.lukeslog.trashplay.ui;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;

import java.util.List;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.playlist.PlayList;
import de.lukeslog.trashplay.playlist.PlayListHelper;
import de.lukeslog.trashplay.radio.RadioManager;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.support.Logger;
import de.lukeslog.trashplay.support.SettingsConstants;

public class SettingsFragment extends PreferenceFragment {

    public static final String TAG = TrashPlayConstants.TAG;

    PlayList activeRemotePlayList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "preferences...");

        addPreferencesFromResource(R.xml.preferences);

        trashModeListener();

        radioSettings();

        appInformation();

        try {
            playlistactivationsettings();
        } catch (Exception e) {
            Logger.d(TAG, "Exeption while playlistactivationsettings()");
        }


        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }

    private void appInformation() {
        Preference pref = getPreferenceManager().findPreference("pref_infoemation_on_app");

        PackageInfo pinfo = getPackageInfo();

        String versionName = pinfo.versionName;

        pref.setSummary("You are using TrashPlay " + versionName + ".");
    }

    private PackageInfo getPackageInfo() {
        PackageInfo pInfo = null;
        try {
            pInfo = TrashPlayService.getContext().getPackageManager().getPackageInfo(TrashPlayService.getContext().getPackageName(), 0);
        } catch (Exception e) {

        }
        return pInfo;
    }

    private void radioSettings() {
        Logger.d(TAG, "radiosettings");
        boolean enable = radioSettingsEnabled();
        enableRadioSettings(enable);
        fillRadioSettings();
    }

    private void fillRadioSettings() {
        Logger.d(TAG, "fillRadioStation");
        if (TrashPlayService.serviceRunning()) {
            localPlayListIsDeactivated();
            final SharedPreferences settings = TrashPlayService.getDefaultSettings();
            if (activeRemotePlayList != null) {
                Logger.d(TAG, "Radio_" + activeRemotePlayList.getRemoteStorage() + "_" + activeRemotePlayList.getRemotePath());
                String stations = settings.getString("Radio_" + activeRemotePlayList.getRemoteStorage() + "_" + activeRemotePlayList.getRemotePath(), "");
                Logger.d(TAG, "stations");
                stations.replace("_", "");
                String[] theStations = stations.split("XX88XX88XX");
                for (final String station : theStations) {
                    String stationName = station.replace(".station", "");
                    if (!stationName.equals("")) {
                        PreferenceScreen pref = (PreferenceScreen) getPreferenceManager().findPreference("pref_app_detail_setting");
                        CheckBoxPreference radio = new CheckBoxPreference(getActivity());
                        radio.setKey("Radio_" + activeRemotePlayList.getRemoteStorage() + "_" + activeRemotePlayList.getRemotePath() + "_" + station);
                        radio.setEnabled(true);
                        radio.setSummary("Check if you want to listen to this radio station.");
                        radio.setTitle(stationName);
                        try {
                            Preference oldpref = getPreferenceManager().findPreference("Radio_" + activeRemotePlayList.getRemoteStorage() + "_" + activeRemotePlayList.getRemotePath() + "_" + station);
                            pref.removePreference(oldpref);
                        } catch (Exception e) {

                        }
                        radio.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                            
                            @Override
                            public boolean onPreferenceChange(Preference preference, Object o) {
                                CheckBoxPreference x = (CheckBoxPreference) preference;
                                if (!x.isChecked()) {
                                    Logger.d(TAG, "YOU JUST ACTIVATED A RADIO");
                                    storePlayListToTemp();
                                    SharedPreferences.Editor edit = settings.edit();
                                    edit.putBoolean("listenalong", true);
                                    edit.putString("radiostation", "Radio_" + activeRemotePlayList.getRemoteStorage() + "_" + activeRemotePlayList.getRemotePath() + "_" + station);
                                    edit.commit();

                                    TrashPlayService.getContext().sendBroadcastToStopMusic();
                                    new Thread(new Runnable() {
                                        public void run() {
                                            try {
                                                MusicCollectionManager.getInstance().getRadioStationFile();
                                                TrashPlayService.getContext().sendBroadcastToStartMusic();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }).start();
                                } else {
                                    Logger.d(TAG, "YOU JUST DEACTIVATED A RADIO");
                                    restorePlayListFromTemp();
                                        RadioManager.setRadioMode(false);
                                }
                                return true;
                            }
                        });
                        pref.addPreference(radio);
                    }
                }
            } else {
                Logger.d(TAG, "activeplaylist is null which makes no sense what so ever.");
            }
        }

    }

    private void enableRadioSettings(boolean enable) {
        Preference radioModePreference = getPreferenceManager().findPreference("pref_appdata_radiomode");
        if (enable) {
            radioModePreference.setEnabled(true);
            radioModePreference.setSummary("Check if you want to broadcast.");
        } else {
            radioModePreference.setEnabled(false);
            radioModePreference.setSummary("You can only broadcast if you have only one active playlist that is not local.");
        }
        radioModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                CheckBoxPreference x = (CheckBoxPreference) preference;
                if (!x.isChecked()) {
                    Logger.d(TAG, "YOU JUST ACTIVATED RADIO MODE");
                    try {

                        MusicCollectionManager.getInstance().updateRadioFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Logger.d(TAG, "YOU JUST DEACTIVATED RADIO MODE");
                }
                return true;
            }
        });
    }

    private boolean radioSettingsEnabled() {
        EditTextPreference radioName = (EditTextPreference) getPreferenceManager().findPreference(SettingsConstants.APP_SETTING_RADIO_NAME);
        radioName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                EditTextPreference preference1 = (EditTextPreference) preference;
                SharedPreferences settings = TrashPlayService.getDefaultSettings();
                SharedPreferences.Editor edit = settings.edit();
                edit.putString(SettingsConstants.APP_SETTING_RADIO_NAME, preference1.getEditText().getText().toString());
                edit.commit();
                radioSettings();
                return true;
            }
        });


        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        String radioNameFromSettings = settings.getString(SettingsConstants.APP_SETTING_RADIO_NAME, "");
        boolean trashmode = settings.getBoolean(SettingsConstants.APP_SETTINGS_TRASHMODE, true);
        if (!trashmode) {
            return false;
        }
        radioName.setText(radioNameFromSettings);

        if (!radioNameFromSettings.equals("")) {
            int i = getNumberOfActivePlayLists();
            Logger.d(TAG, "numberOfActivePlaylists" + i);
            if (i == 1) {
                boolean localPlayListsDeactivated = localPlayListIsDeactivated();
                if (localPlayListsDeactivated) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean localPlayListIsDeactivated() {
        List<PlayList> playLists = PlayListHelper.getAllPlayLists();
        for (PlayList playList : playLists) {
            if (playList.isActivated()) {
                activeRemotePlayList = playList;
            }
            if (playList.getRemoteStorage().equals(StorageManager.STORAGE_TYPE_LOCAL) && playList.isActivated()) {
                return false;
            }
        }
        return true;
    }

    private void playlistactivationsettings() throws Exception {
        final PreferenceScreen pref = (PreferenceScreen) getPreferenceManager().findPreference("pref_app_playlistusage_settings");
        List<PlayList> playLists = PlayListHelper.getAllPlayLists();
        for (final PlayList playList : playLists) {
            Logger.d(TAG, "playlistactivationsettings() for Playlist " + playList.getRemotePath());
            new Thread(new Runnable() {
                public void run() {
                    try {
                        StorageManager.getStorage(playList.getRemoteStorage()).getRadioStations(playList.getRemotePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            final CheckBoxPreference playlistActivationSetting = new CheckBoxPreference(getActivity());
            SharedPreferences settings = TrashPlayService.getDefaultSettings();
            SharedPreferences.Editor edit = settings.edit();
            edit.putBoolean("pref_activateplaylist_" + playList.getRemoteStorage() + "_" + playList.getRemotePath(), playList.isActivated());
            edit.commit();
            playlistActivationSetting.setKey("pref_activateplaylist_" + playList.getRemoteStorage() + "_" + playList.getRemotePath());
            playlistActivationSetting.setEnabled(true);
            int n = 0;
            n = PlayListHelper.getNumberOfSongsInPlayList(playList);
            playlistActivationSetting.setChecked(playList.isActivated());
            playlistActivationSetting.setSummary("Check if you want to use this playlist\n" + n + " songs.");
            playlistActivationSetting.setTitle(playList.getRemotePath() + " (" + playList.getRemoteStorage() + ")");
            playlistActivationSetting.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    CheckBoxPreference x = (CheckBoxPreference) preference;
                    if (!x.isChecked()) {
                        new Thread(new Runnable() {
                            public void run() {
                                PlayListHelper.setActivated(playList, true);
                            }
                        }).start();
                    } else {
                        new Thread(new Runnable() {
                            public void run() {
                                PlayListHelper.setActivated(playList, false);
                            }
                        }).start();
                    }
                    return true;
                }
            });
            playlistActivationSetting.setChecked(playList.isActivated());

            pref.addPreference(playlistActivationSetting);

            if(!playList.isActivated()){
                Preference deletePlaylist = new Preference(getActivity());
                deletePlaylist.setSummary("Delete Playlist");
                deletePlaylist.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Logger.d(TAG, "try to delete a playlist");
                        PlayListHelper.removePlaylist(playList);
                        playlistActivationSetting.setEnabled(false);
                        playlistActivationSetting.setSummary("Deleted");
                        preference.setEnabled(false);
                        preference.setSummary("");
                        return true;
                    }
                });
                pref.addPreference(deletePlaylist);
            }
        }
    }

    public int getNumberOfActivePlayLists() {
        return MusicCollectionManager.getInstance().getNumberOfActivePlayLists();

    }

    public void trashModeListener() {

        CheckBoxPreference useTrashMode = (CheckBoxPreference) getPreferenceManager().findPreference("pref_appdata_trashmode");

        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        boolean listenalong = settings.getBoolean("listenalong", false);
        boolean radioMode = settings.getBoolean(SettingsConstants.APP_SETTING_RADIO_MODE, false);

        if (listenalong || radioMode) {
            useTrashMode.setEnabled(false);
        } else {
            useTrashMode.setEnabled(true);
        }
        useTrashMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                CheckBoxPreference x = (CheckBoxPreference) preference;
                if (!x.isChecked()) {
                    Logger.d(TAG, "YOU JUST ACTIVATED TRASH MODE");

                    TrashPlayService.getContext().sendBroadcastToStopMusic();
                    MusicCollectionManager.getInstance().resetNextSongs();
                    storePlayListToTemp();
                    TrashPlayService.getContext().sendBroadcastToStartMusic();

                } else {
                    restorePlayListFromTemp();
                }
                return true;
            }
        });
    }

    private void storePlayListToTemp() {
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        if (settings != null) {
            SharedPreferences.Editor edit = settings.edit();
            for (int i = 0; i < 10; i++) {
                String nextSongFileName = settings.getString("nextSong_" + i, "");
                edit.putString("nextSongTEMP_" + i, nextSongFileName);
            }
            edit.commit();
        }
    }

    private void restorePlayListFromTemp() {
        SharedPreferences settings = TrashPlayService.getDefaultSettings();
        if (settings != null) {
            SharedPreferences.Editor edit = settings.edit();
            for (int i = 0; i < 10; i++) {
                String nextSongFileName = settings.getString("nextSongTEMP_" + i, "");
                edit.putString("nextSong_" + i, nextSongFileName);
            }
            edit.commit();
        }
    }
}

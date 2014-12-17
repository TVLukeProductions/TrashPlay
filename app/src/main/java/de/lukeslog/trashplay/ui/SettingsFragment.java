package de.lukeslog.trashplay.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.util.List;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.cloudstorage.StorageManager;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.playlist.PlayList;
import de.lukeslog.trashplay.playlist.PlayListHelper;
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

        try {
            playlistactivationsettings();
        } catch (Exception e) {
            Logger.d(TAG, "Exeption while playlistactivationsettings()");
        }


        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
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
                                    SharedPreferences.Editor edit = settings.edit();
                                    edit.putBoolean("listenalong", false);
                                    edit.commit();
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
            if (playList.getRemoteStorage().equals(StorageManager.LOCAL_STORAGE) && playList.isActivated()) {
                return false;
            }
        }
        return true;
    }

    private void playlistactivationsettings() throws Exception {
        PreferenceScreen pref = (PreferenceScreen) getPreferenceManager().findPreference("pref_app_playlistusage_settings");
        List<PlayList> playLists = PlayListHelper.getAllPlayLists();
        for (final PlayList playList : playLists) {
            Logger.d(TAG, "playlistactivationsettings() for Playlist "+playList.getRemotePath());
            new Thread(new Runnable() {
                public void run() {
                    try {
                        StorageManager.getStorage(playList.getRemoteStorage()).getRadioStations(playList.getRemotePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            CheckBoxPreference playlistActivationSetting = new CheckBoxPreference(getActivity());
            playlistActivationSetting.setKey("pref_activateplaylist_" + playList.getRemoteStorage() + "_" + playList.getRemotePath());
            playlistActivationSetting.setEnabled(true);
            int n=0;
            n= PlayListHelper.getNumberOfSongsInPlayList(playList);
            playlistActivationSetting.setSummary("Check if you want to use this playlist\n"+n+" songs.");
            playlistActivationSetting.setTitle(playList.getRemotePath() + " (" + playList.getRemoteStorage() + ")");
            playlistActivationSetting.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    CheckBoxPreference x = (CheckBoxPreference) preference;
                    if (!x.isChecked()) {
                        PlayListHelper.setActivated(playList, true);
                    } else {
                        PlayListHelper.setActivated(playList, false);
                    }
                    return true;
                }
            });
            playlistActivationSetting.setChecked(playList.isActivated());
            pref.addPreference(playlistActivationSetting);
        }
    }

    public int getNumberOfActivePlayLists() {
        return MusicCollectionManager.getInstance().getNumberOfActivePlayLists();

    }

    public void trashModeListener() {

        CheckBoxPreference useTrashMode = (CheckBoxPreference) getPreferenceManager().findPreference("pref_appdata_trashmode");
        useTrashMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                CheckBoxPreference x = (CheckBoxPreference) preference;
                if (!x.isChecked()) {
                    Logger.d(TAG, "YOU JUST ACTIVATED TRASH MODE");

                    TrashPlayService.getContext().sendBroadcastToStopMusic();
                    MusicCollectionManager.getInstance().resetNextSongs();
                    SharedPreferences settings = TrashPlayService.getDefaultSettings();
                    if (settings != null) {
                        SharedPreferences.Editor edit = settings.edit();
                        for (int i = 0; i < 10; i++) {
                            String nextSongFileName = settings.getString("nextSongTEMP_" + i, "");
                            edit.putString("nextSong_" + i, nextSongFileName);
                        }
                        edit.commit();
                        TrashPlayService.getContext().sendBroadcastToStartMusic();
                    }

                } else {
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
                return true;
            }
        });
    }
}

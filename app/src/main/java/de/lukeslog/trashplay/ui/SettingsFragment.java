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

public class SettingsFragment extends PreferenceFragment {

    public static final String TAG = TrashPlayConstants.TAG;

    PlayList activeRemotePlayList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "preferences...");
        addPreferencesFromResource(R.xml.preferences);

        radioSettings();

        playlistactivationsettings();


        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }

    private void radioSettings() {
        Log.d(TAG, "radiosettings");
        boolean enable = radioSettingsEnabled();
        enableRadioSettings(enable);
        fillRadioSettings();
    }

    private void fillRadioSettings() {
        Log.d(TAG, "fillRadioStation");
        if(TrashPlayService.serviceRunning()) {
            localPlayListIsDeactivated();
            final SharedPreferences settings = TrashPlayService.getDefaultSettings();
            if (activeRemotePlayList != null) {
                Log.d(TAG, "Radio_" + activeRemotePlayList.getRemoteStorage() + "_" + activeRemotePlayList.getRemotePath());
                String stations = settings.getString("Radio_" + activeRemotePlayList.getRemoteStorage() + "_" + activeRemotePlayList.getRemotePath(), "");
                Log.d(TAG, "stations");
                stations.replace("_", "");
                String[] theStations = stations.split(" ");
                for (final String station : theStations) {
                    PreferenceScreen pref = (PreferenceScreen) getPreferenceManager().findPreference("pref_app_detail_setting");
                    CheckBoxPreference radio = new CheckBoxPreference(getActivity());
                    radio.setKey("Radio_" + activeRemotePlayList.getRemoteStorage() + "_" + activeRemotePlayList.getRemotePath() + "_" + station);
                    radio.setEnabled(true);
                    radio.setSummary("Check if you want to use this playlist");
                    String stationName = station.replace(".station", "");
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
                                Log.d(TAG, "YOU JUST ACTIVATED A RADIO");
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
                                Log.d(TAG, "YOU JUST DEACTIVATED A RADIO");
                                SharedPreferences.Editor edit = settings.edit();
                                edit.putBoolean("listenalong", false);
                                edit.commit();
                            }
                            return true;
                        }
                    });
                    pref.addPreference(radio);
                }
            } else
            {
                Log.d(TAG, "activeplaylist is null which makes no sense what so ever.");
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
                    Log.d(TAG, "YOU JUST ACTIVATED RADIO MODE");
                    try {
                        MusicCollectionManager.getInstance().updateRadioFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d(TAG, "YOU JUST DEACTIVATED RADIO MODE");
                }
                return true;
            }
        });
    }

    private boolean radioSettingsEnabled() {
        EditTextPreference radioName = (EditTextPreference) getPreferenceManager().findPreference("pref_appdata_radioname");
        radioName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                radioSettings();
                return true;
            }
        });
        String radioNameString = radioName.getEditText().getText().toString();
        if(!radioNameString.equals("")) {
            int i = getNumberOfActivePlayLists();
            Log.d(TAG, "numberOfActivePlaylists" + i);
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
            if(playList.isActivated()) {
                activeRemotePlayList = playList;
            }
            if (playList.getRemoteStorage().equals(StorageManager.LOCAL_STORAGE) && playList.isActivated()) {
                return false;
            }
        }
        return true;
    }

    private void playlistactivationsettings() {
        PreferenceScreen pref = (PreferenceScreen) getPreferenceManager().findPreference("pref_app_playlistusage_settings");
        List<PlayList> playLists = PlayListHelper.getAllPlayLists();
        for (final PlayList playList : playLists) {
            CheckBoxPreference playlistActivationSetting = new CheckBoxPreference(getActivity());
            playlistActivationSetting.setKey("pref_activateplaylist_" + playList.getRemoteStorage() + "_" + playList.getRemotePath());
            playlistActivationSetting.setEnabled(true);
            playlistActivationSetting.setSummary("Check if you want to use this playlist");
            playlistActivationSetting.setTitle(playList.getRemotePath() + " (" + playList.getRemoteStorage() + ")");
            playlistActivationSetting.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    CheckBoxPreference x = (CheckBoxPreference) preference;
                    if (!x.isChecked()) {
                        PlayListHelper.activated(playList, true);
                    } else {
                        PlayListHelper.activated(playList, false);
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
}

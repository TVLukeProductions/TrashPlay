package de.lukeslog.trashplay.ui;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.ArrayList;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.playlist.MusicCollectionManager;
import de.lukeslog.trashplay.playlist.PlayList;
import de.lukeslog.trashplay.playlist.PlayListFileSynchronizer;

public class SettingsFragment extends PreferenceFragment {

    public static final String TAG = TrashPlayConstants.TAG;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceScreen pref = (PreferenceScreen) getPreferenceManager().findPreference("pref_app_playlistusage_settings");
        ArrayList<PlayList> playLists = MusicCollectionManager.getInstance().getAllPlayLists();
        for(final PlayList playList : playLists) {
            CheckBoxPreference playlistActivationSetting = new CheckBoxPreference(getActivity());
            playlistActivationSetting.setKey("pref_activateplaylist_" + playList.getRemoteStorage() + "_" + playList.getRemotePath());
            playlistActivationSetting.setEnabled(true);
            playlistActivationSetting.setSummary("Check if you want to use this playlist");
            playlistActivationSetting.setTitle(playList.getRemotePath() + " (" + playList.getRemoteStorage() + ")");
            playlistActivationSetting.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    CheckBoxPreference x = (CheckBoxPreference) preference;
                    if(!x.isChecked()) {
                        PlayListFileSynchronizer.activated(playList, true);
                    } else {
                        PlayListFileSynchronizer.activated(playList, false);
                    }
                    return true;
                }
            });
            playlistActivationSetting.setChecked(playList.isActivated());
            pref.addPreference(playlistActivationSetting);
        }


        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }
}

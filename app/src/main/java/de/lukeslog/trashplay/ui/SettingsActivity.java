package de.lukeslog.trashplay.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

@SuppressLint("CutPasteId")
public class SettingsActivity extends Activity {

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
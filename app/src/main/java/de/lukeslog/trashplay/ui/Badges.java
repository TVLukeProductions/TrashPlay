package de.lukeslog.trashplay.ui;

import android.app.Activity;
import android.os.Bundle;

import com.activeandroid.ActiveAndroid;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.support.Logger;

public class Badges extends Activity {

    public static final String TAG = TrashPlayConstants.TAG;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "on Create Badges");
        setContentView(R.layout.activity_badges);
    }
}

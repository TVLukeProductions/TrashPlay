package de.lukeslog.trashplay.ui;

import android.app.Activity;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.widget.EditText;
import android.widget.TextView;

import com.activeandroid.ActiveAndroid;

import java.util.List;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.playlist.SongHelper;
import de.lukeslog.trashplay.statistics.StatisticsCollection;
import de.lukeslog.trashplay.support.Logger;

public class Statistics extends Activity {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final String PREFS_NAME = TrashPlayConstants.PREFS_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "on Create Statistics");
        ActiveAndroid.initialize(this);
        setContentView(R.layout.statistics_activity);

        String text = "";
        text = ""+(((StatisticsCollection.getPlayTime()/1000)/60)/60)+" Stunden TrashPlay.\n";

        text = text+"Charts\n";
        List<Song> songsByPlay = SongHelper.getAllSongsOrderedByPlays();
        int counter=0;
        int max =0;
        for(Song song: songsByPlay){
            counter++;
            text = text+ song.getPlays()+". "+song.getArtist()+" - "+song.getSongName()+"\n";
            if(counter==1){
                max = song.getPlays();
            }
            if(counter==10){
                break;
            }
        }

        int[] playdist = new int[max+1];
        if(max>0){
            for(Song song: songsByPlay){
                playdist[song.getPlays()]++;
            }
        }

        text = text +" Play Distribution. \n";
        for(int i=0; i<playdist.length; i++){
            text = text+" "+i+" = "+playdist[i]+"\n";
        }


        TextView stat1 = (TextView) findViewById(R.id.statistics);

        stat1.setText(text);
    }


}

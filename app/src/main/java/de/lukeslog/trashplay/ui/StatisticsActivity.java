package de.lukeslog.trashplay.ui;

import android.app.Activity;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.activeandroid.ActiveAndroid;
import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.*;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import org.joda.time.DateTime;

import java.util.List;

import de.lukeslog.trashplay.R;
import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.playlist.Song;
import de.lukeslog.trashplay.playlist.SongHelper;
import de.lukeslog.trashplay.service.TrashPlayService;
import de.lukeslog.trashplay.statistics.StatisticsCollection;
import de.lukeslog.trashplay.support.Logger;

public class StatisticsActivity extends Activity {

    public static final String TAG = TrashPlayConstants.TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "on Create Statistics");
        setContentView(R.layout.activity_statistics);

        TextView playtime = (TextView) findViewById(R.id.playtime);
        TextView plays = (TextView) findViewById(R.id.playsstats);
        TextView distance = (TextView) findViewById(R.id.distance);
        TextView longesttreak = (TextView) findViewById((R.id.streakmax));

        playtime.setText(""+(((StatisticsCollection.getPlayTime() / 1000) / 60) / 60)+ " hours");
        plays.setText(""+StatisticsCollection.getPlays());
        distance.setText(""+(StatisticsCollection.getTotalDistance()/1000)+" km");
        String longestStreakSongName = StatisticsCollection.getLongestStreakSongName();
        Song longestStreakSong = SongHelper.getSongByFileName(longestStreakSongName);
        if(!longestStreakSongName.equals("") && longestStreakSong!=null) {
            SongHelper.getSongByFileName(longestStreakSongName);
            longesttreak.setText("" + StatisticsCollection.getLongestStreak()+" ("+longestStreakSong.getArtist()+" - "+longestStreakSong.getSongName()+")");
        } else {
            longesttreak.setVisibility(View.GONE);
            TextView longesttreaklabel = (TextView) findViewById((R.id.streaklabel));
            longesttreaklabel.setVisibility(View.GONE);
        }

        Integer[] playsLastTwoWeeks = StatisticsCollection.playsFromTo(new DateTime().minusDays(14), new DateTime());
        GraphViewData[] gvd1 = new GraphViewData[playsLastTwoWeeks.length];
        String[] xLabel = new String[playsLastTwoWeeks.length];
        for(int i=0; i<playsLastTwoWeeks.length; i++) {
            gvd1[i]= new GraphViewData((i+1), playsLastTwoWeeks[i]);
            xLabel[i]="";
        }
        GraphViewSeries exampleSeries = new GraphViewSeries(gvd1);

        GraphView graphView = new BarGraphView(
                this // context
                , "Last Two Weeks" // heading
        );
        graphView.addSeries(exampleSeries); // data
        graphView.setHorizontalLabels(xLabel);
        LinearLayout layout = (LinearLayout) findViewById(R.id.twoweekstats);
        layout.addView(graphView);


        LinearLayout charts = (LinearLayout) findViewById(R.id.charts);
        String text =  "Charts\n";
        List<Song> songsByPlay = SongHelper.getAllSongsOrderedByPlays();
        int counter = 0;
        int max = 0;
        for (Song song : songsByPlay) {
            if(counter<11) {
            counter++;
                TextView s = new TextView(TrashPlayService.getContext());
                s.setText(counter+" "+song.getArtist() + " - " + song.getSongName()+" ("+song.getPlays()+")");
                if(counter%2==0){
                    s.setBackgroundColor(0xFFA4F373);
                }
                charts.addView(s);
            }
            text = text + song.getPlays() + ". " + song.getArtist() + " - " + song.getSongName() + "\n";
            if (counter == 1) {
                max = song.getPlays();
            }
            if (counter == 10) {
                break;
            }
        }

        int[] playdist = new int[max + 1];
        String[] xLabels = new String[max + 1];
        if (max > 0) {
            for (Song song : songsByPlay) {
                playdist[song.getPlays()]++;
            }
        }
        GraphViewData[] gvd2 = new GraphViewData[max+1];
        for (int i = 0; i < playdist.length; i++) {
            gvd2[i]= new GraphViewData((i+1), playdist[i]);
            xLabels[i]=""+i+"";
        }

        GraphViewSeries distributionSeries = new GraphViewSeries(gvd2);

        GraphView distributionView = new BarGraphView(
                this // context
                , "Play Distribution" // heading
        );
        distributionView.addSeries(distributionSeries); // data

        distributionView.setHorizontalLabels(xLabels);

        LinearLayout distView = (LinearLayout) findViewById(R.id.distribution);
        distView .addView(distributionView);
    }


}

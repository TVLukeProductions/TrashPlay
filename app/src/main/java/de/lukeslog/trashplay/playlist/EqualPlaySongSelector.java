package de.lukeslog.trashplay.playlist;

import android.util.Log;

import java.util.ArrayList;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.support.Logger;

public class EqualPlaySongSelector extends SongSelector {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final int AT_LEAST_X_SONGS=20;

    @Override
    public Song getASong() {
        ArrayList<Song> songs = MusicCollectionManager.getInstance().getListOfSongs();
        ArrayList<Song> songsWithTheLeastPlay = getSongsWithTheLeastPlay(songs);
        if(!songsWithTheLeastPlay.isEmpty()) {
            int randomSongNumber = (int) (Math.random() * (songsWithTheLeastPlay.size()));
            Song[] theSongs = songsWithTheLeastPlay.toArray(new Song[songsWithTheLeastPlay.size()]);
            Song possibleSong = theSongs[randomSongNumber];
            Logger.d(TAG, possibleSong.getFileName());
            return possibleSong;
        }
        return null;
    }

    private ArrayList<Song> getSongsWithTheLeastPlay(ArrayList<Song> songs) {
        ArrayList<Song> songsWithLeastPlay = new ArrayList<Song>();
        if(songs.size()>0) {
            int lowestPlayCount = getLowestPlayCount(songs);
            songsWithLeastPlay = getSongsWithNPlays(songs, lowestPlayCount);
            int max = AT_LEAST_X_SONGS;
            if (songs.size() < AT_LEAST_X_SONGS) {
                max = songs.size();
            }
            while (songsWithLeastPlay.size() < max) {
                lowestPlayCount++;
                songsWithLeastPlay.addAll(getSongsWithNPlays(songs, lowestPlayCount));
            }
        }
        return songsWithLeastPlay;
    }

    private ArrayList<Song> getSongsWithNPlays(ArrayList<Song> songs, int playCount) {
        ArrayList<Song> songsWithNPlays = new ArrayList<Song>();
        for(Song song : songs) {
            if(song.getPlays()==playCount) {
                songsWithNPlays.add(song);
            }
        }
        return songsWithNPlays;
    }

    private int getLowestPlayCount(ArrayList<Song> songs) {
        int lowest = songs.get(0).getPlays();
        for(Song song : songs) {
            if(song.getPlays()<lowest) {
                lowest=song.getPlays();
            }
        }
        return lowest;
    }
}

package de.lukeslog.trashplay.playlist;

import java.util.ArrayList;
import java.util.List;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.support.Logger;

public class EqualPlaySongSelector extends SongSelector {

    public static final String TAG = TrashPlayConstants.TAG;
    public static final int AT_LEAST_X_SONGS=20;

    @Override
    public Song getASong() {
        List<Song> songs = SongHelper.getAllSongs();
        List<Song> songsWithTheLeastPlay = getSongsWithTheLeastPlay(songs);
        if(!songsWithTheLeastPlay.isEmpty()) {
            int randomSongNumber = (int) (Math.random() * (songsWithTheLeastPlay.size()));
            Song[] theSongs = songsWithTheLeastPlay.toArray(new Song[songsWithTheLeastPlay.size()]);
            Song possibleSong = theSongs[randomSongNumber];
            Logger.d(TAG, possibleSong.getFileName());
            return possibleSong;
        }
        return null;
    }

    @Override
    public String getSelectorName() {
        return "Equal Play Selector";
    }

    @Override
    public String getSelectorDescription() {
        return "Mostly random but with a preference for songs played less.";
    }

    private List<Song> getSongsWithTheLeastPlay(List<Song> songs) {
        ArrayList<Song> songsWithLeastPlay = new ArrayList<Song>();
        if(songs.size()>0) {
            int lowestPlayCount = getLowestPlayCount(songs);
            lowestPlayCount=lowestPlayCount+5;
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

    private ArrayList<Song> getSongsWithNPlays(List<Song> songs, int playCount) {
        ArrayList<Song> songsWithNPlays = new ArrayList<Song>();
        for(Song song : songs) {
            if(song.getPlays()<=playCount) {
                songsWithNPlays.add(song);
            }
        }
        return songsWithNPlays;
    }

    private int getLowestPlayCount(List<Song> songs) {
        int lowest = songs.get(0).getPlays();
        for(Song song : songs) {
            if(song.getPlays()<lowest) {
                lowest=song.getPlays();
            }
        }
        return lowest;
    }
}

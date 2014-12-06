package de.lukeslog.trashplay.playlist;

import java.util.ArrayList;
import java.util.HashMap;

public class EqualPlaySongSelector extends SongSelector {

    public static final int AT_LEAST_X_SONGS=20;
    @Override
    public Song getASong() {
        HashMap<String, Song> songst = MusicCollectionManager.getInstance().getListOfSongs();
        ArrayList<Song> songs = new ArrayList<Song>();
        for(Song song : songst.values()) {
            songs.add(song);
        }
        ArrayList<Song> songsWithTheLeastPlay = getSongsWithTheLeastPlay(songs);

        int randomSongNumber = (int) (Math.random() * (songsWithTheLeastPlay.size()));
        Song[] theSongs = songsWithTheLeastPlay.toArray(new Song[songsWithTheLeastPlay.size()]);
        Song possibleSong = theSongs[randomSongNumber];
        return possibleSong;
    }

    private ArrayList<Song> getSongsWithTheLeastPlay(ArrayList<Song> songs) {
        int lowestPlayCount = getLowestPlayCount(songs);
        ArrayList<Song> songsWithLeastPlay = getSongsWithNPlays(songs, lowestPlayCount);
        int max = AT_LEAST_X_SONGS;
        if(songs.size()<AT_LEAST_X_SONGS) {
            max = songs.size();
        }
        while(songsWithLeastPlay.size()<max) {
            lowestPlayCount++;
            songsWithLeastPlay.addAll(getSongsWithNPlays(songs, lowestPlayCount));
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

package de.lukeslog.trashplay.playlist;

import java.util.List;

public class DefaultSongSelector extends SongSelector {

    public Song getASong() {
        List<Song> songs = SongHelper.getAllSongs();
        int randomSongNumber = (int) (Math.random() * (songs.size()));
        Song[] theSongs = songs.toArray(new Song[songs.size()]);
        Song possibleSong = theSongs[randomSongNumber];
        return possibleSong;
    }
}

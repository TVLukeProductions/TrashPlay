package de.lukeslog.trashplay.playlist;

import java.util.ArrayList;
import java.util.HashMap;

public class DefaultSongSelector extends SongSelector {

    public Song getASong() {
        ArrayList<Song> songs = MusicCollectionManager.getInstance().getListOfSongs();
        int randomSongNumber = (int) (Math.random() * (songs.size()));
        Song[] theSongs = songs.toArray(new Song[songs.size()]);
        Song possibleSong = theSongs[randomSongNumber];
        return possibleSong;
    }
}

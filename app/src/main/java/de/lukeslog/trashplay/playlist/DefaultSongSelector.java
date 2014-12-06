package de.lukeslog.trashplay.playlist;

import java.util.HashMap;

public class DefaultSongSelector extends SongSelector {

    public Song getASong() {
        HashMap<String, Song> songs = MusicCollectionManager.getInstance().getListOfSongs();
        int randomSongNumber = (int) (Math.random() * (songs.size()));
        Song[] theSongs = songs.values().toArray(new Song[songs.size()]);
        Song possibleSong = theSongs[randomSongNumber];
        return possibleSong;
    }
}

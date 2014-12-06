package de.lukeslog.trashplay.playlist;

public class SongSelectorFactory {

    public static SongSelector getSongSelector(String songSelectorClass) {
        if(songSelectorClass.equals(SongSelector.EQUAL_PLAY_SONG_SELECTOR)) {
            return new EqualPlaySongSelector();
        }
        return new DefaultSongSelector();
    }

    public static SongSelector getSongSelector() {
        return getSongSelector("");
    }
}

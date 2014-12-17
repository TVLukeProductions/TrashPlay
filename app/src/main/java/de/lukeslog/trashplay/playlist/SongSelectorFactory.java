package de.lukeslog.trashplay.playlist;

import de.lukeslog.trashplay.constants.TrashPlayConstants;
import de.lukeslog.trashplay.support.Logger;

public class SongSelectorFactory {

    public static final String TAG = TrashPlayConstants.TAG;

    public static SongSelector getSongSelector(String songSelectorClass) {
        if(songSelectorClass.equals(SongSelector.EQUAL_PLAY_SONG_SELECTOR)) {
            Logger.d(TAG, "Equals Play");
            return new EqualPlaySongSelector();
        }
        Logger.d(TAG, "Deafult Seletor");
        return new DefaultSongSelector();
    }

    public static SongSelector getSongSelector() {
        return getSongSelector("");
    }
}

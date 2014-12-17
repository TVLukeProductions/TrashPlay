package de.lukeslog.trashplay.playlist;


public abstract class SongSelector {

    public static final String DEFAULT_SONG_SELECTOR ="DefaultSongSelector";
    public static final String EQUAL_PLAY_SONG_SELECTOR ="EqualPlaySongSelector";

    public abstract Song getASong();

    public abstract String getSelectorName();

    public abstract String getSelectorDescription();
}

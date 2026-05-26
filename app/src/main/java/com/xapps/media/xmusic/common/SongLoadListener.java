package com.xapps.media.xmusic.common;

import com.xapps.media.xmusic.models.Song;
import java.util.ArrayList;
import java.util.HashMap;

public interface SongLoadListener {

    default void onStarted(int totalSongs) {}

    default void onProgress(ArrayList<Song> songs, int count) {}

    default void onComplete(ArrayList<Song> songs) {}
}
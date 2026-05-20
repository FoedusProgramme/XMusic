package com.xapps.media.xmusic.common;

import com.xapps.media.xmusic.models.Song;
import java.util.ArrayList;
import java.util.HashMap;

public interface SongLoadListener {

    default void onStarted(int totalSongs) {}

    default void onProgress(ArrayList<HashMap<String, Object>> songs, int count) {}

    default void onComplete(ArrayList<HashMap<String, Object>> songs) {}

    default void onProgressNew(ArrayList<Song> songs, int count) {}

    default void onCompleteNew(ArrayList<Song> songs) {}
}
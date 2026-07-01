package com.xapps.media.xmusic.callback;

public interface ActivityCallback {

    default void onSongChanged(int position) {
        
    }

    default void onProgressChanged(long progress) {
        
    }

    default void onColorsChanged() {
        
    }

    default void onPlaybackStateChanged(boolean playing) {
        
    }
}
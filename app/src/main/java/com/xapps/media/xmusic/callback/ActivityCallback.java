package com.xapps.media.xmusic.callback;
import com.xapps.media.xmusic.fragment.base.*;
import com.xapps.media.xmusic.fragment.main.*;
import com.xapps.media.xmusic.fragment.settings.*;
import com.xapps.media.xmusic.fragment.info.*;
import com.xapps.media.xmusic.ui.widget.*;
import com.xapps.media.xmusic.ui.lyrics.*;
import com.xapps.media.xmusic.ui.scroll.*;
import com.xapps.media.xmusic.ui.behavior.*;
import com.xapps.media.xmusic.lyrics.*;
import com.xapps.media.xmusic.callback.*;
import com.xapps.media.xmusic.models.*;

public interface ActivityCallback {

    default void onSongChanged(int position) {
        
    }

    default void onProgressChanged(long progress) {
        
    }

    default void onColorsChanged() {
        
    }

    default void onPlaybackStateChanged(boolean playing) {
        
    }

    default void updateState() {
        
    }

    default void passData() {
        
    }

    default void onNotificationButtonsUsed(int i) {

    }

    default void onNotificationButtonsUsed(boolean b) {

    }

    default void onPlayerReady() {

    }
}
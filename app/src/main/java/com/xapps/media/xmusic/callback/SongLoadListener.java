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

import com.xapps.media.xmusic.models.Song;
import java.util.ArrayList;
import java.util.function.Consumer;

public interface SongLoadListener {

    default void onStarted(int totalSongs) {}

    default void onProgress(ArrayList<Song> songs, int count) {}

    default void onComplete(ArrayList<Song> songs) {}
    
    default void onError(Exception e) {}

    static SongLoadListener on(Consumer<ArrayList<Song>> complete, Consumer<Exception> error) {
        return new SongLoadListener() {
            @Override
            public void onComplete(ArrayList<Song> songs) {
                complete.accept(songs);
            }

            @Override
            public void onError(Exception e) {
                error.accept(e);
            }
        };
    }
}

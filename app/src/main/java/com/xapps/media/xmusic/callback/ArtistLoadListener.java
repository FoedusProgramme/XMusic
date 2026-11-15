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

import com.xapps.media.xmusic.models.Artist;
import java.util.ArrayList;
import java.util.function.Consumer;

public interface ArtistLoadListener {

    default void onStarted(int totalArtists) {}

    default void onProgress(ArrayList<Artist> artists, int count) {}

    default void onComplete(ArrayList<Artist> artists) {}

    default void onError(Exception e) {}

    static ArtistLoadListener on(Consumer<ArrayList<Artist>> complete, Consumer<Exception> error) {
        return new ArtistLoadListener() {
            @Override
            public void onComplete(ArrayList<Artist> artists) {
                complete.accept(artists);
            }

            @Override
            public void onError(Exception e) {
                error.accept(e);
            }
        };
    }
}

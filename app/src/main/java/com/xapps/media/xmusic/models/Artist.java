package com.xapps.media.xmusic.models;
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

import java.util.Locale;

public class Artist {

    public final long id;
    public final String name;
    public final int numberOfAlbums;
    public final int numberOfTracks;

    private String searchKey;

    public Artist(long id, String name, int numberOfAlbums, int numberOfTracks) {
        this.id = id;
        this.name = name;
        this.numberOfAlbums = numberOfAlbums;
        this.numberOfTracks = numberOfTracks;
    }

    public String getSearchKey() {
        if (searchKey == null) {
            searchKey = (name == null ? "" : name).toLowerCase(Locale.ROOT);
        }
        return searchKey;
    }
}

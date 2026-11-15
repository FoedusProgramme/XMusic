package com.xapps.media.xmusic.service.resume;
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

import java.io.Serializable;

public class ResumeSong implements Serializable {
    private static final long serialVersionUID = 1L;

    public String title;
    public String artist;
    public String path;
    public String artworkUri;

    public ResumeSong() {
    }

    public ResumeSong(String title, String artist, String path, String artworkUri) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.artworkUri = artworkUri;
    }
}
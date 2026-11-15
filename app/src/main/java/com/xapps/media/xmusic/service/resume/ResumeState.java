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
import java.util.ArrayList;

public class ResumeState implements Serializable {
    private static final long serialVersionUID = 1L;

    public int version = 1;
    public boolean shuffle;
    public int repeatMode;
    public int currentIndex;
    public long positionMs;
    public float speed = 1.0f;
    public ArrayList<ResumeSong> songs = new ArrayList<>();

    public ResumeState() {
    }

    public ResumeState(
            boolean shuffle,
            int repeatMode,
            int currentIndex,
            long positionMs,
            float speed,
            ArrayList<ResumeSong> songs
    ) {
        this.shuffle = shuffle;
        this.repeatMode = repeatMode;
        this.currentIndex = currentIndex;
        this.positionMs = positionMs;
        this.speed = speed;
        this.songs = songs;
    }
}
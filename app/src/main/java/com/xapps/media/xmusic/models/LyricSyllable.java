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

public class LyricSyllable {
    public int startTime;
    public int endTime;
    public int nextStartTime;

    public final String text;
    public final int relStart;

    public LyricSyllable(int startTime, String text, int relStart) {
        this.startTime = startTime;
        this.text = text;
        this.relStart = relStart;
    }
}
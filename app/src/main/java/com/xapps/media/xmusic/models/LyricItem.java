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

public class LyricItem {
    public LyricLine mainLine;
    public int mainIndex;
    public LyricLine romajiLine;
    public int romajiIndex = -1;
    public LyricLine linkedBgLine;
    public int linkedBgIndex = -1;

    public LyricItem(LyricLine main, int idx) {
        this.mainLine = main;
        this.mainIndex = idx;
    }
}
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

import java.util.ArrayList;
import java.util.List;

public class LyricWord {

    public final int startIndex;
    public final List<LyricSyllable> syllables = new ArrayList<>();

    public LyricWord(int startIndex) {
        this.startIndex = startIndex;
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (LyricSyllable s : syllables) sb.append(s.text);
        return sb.toString();
    }

    public int getStartTime() {
        return syllables.get(0).startTime;
    }

    public int getEndTime() {
        return syllables.get(syllables.size() - 1).endTime;
    }
}
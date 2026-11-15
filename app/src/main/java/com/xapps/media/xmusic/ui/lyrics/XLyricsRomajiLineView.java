package com.xapps.media.xmusic.ui.lyrics;
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

import android.content.Context;
import android.util.AttributeSet;

public class XLyricsRomajiLineView extends XLyricsLineView {

    public XLyricsRomajiLineView(Context context) {
        super(context);
        setTranslationZ(-1f);
    }

    public XLyricsRomajiLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTranslationZ(-1f);
    }

    @Override
    public void updateProgress(int progressMs, boolean snap) {
        if (getLyricLine() == null) return;
        super.updateProgress(progressMs < getLyricLine().time ? 0 : Integer.MAX_VALUE, true);
    }
    
    @Override
    public float getExtraPadding() {
        return textPaint.getTextSize() * 0.4f;
    }

}

package com.xapps.media.xmusic.ui.widget;
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
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class MarqueeTextView extends AppCompatTextView {

private boolean marqueeEnabled = true;  

public MarqueeTextView(Context context) {  
    super(context);  
    init();  
}  

public MarqueeTextView(Context context, AttributeSet attrs) {  
    super(context, attrs);  
    init();  
}  

private void init() {  
    setHorizontalFadingEdgeEnabled(true);  
    setFadingEdgeLength(32);  
    setEllipsize(TextUtils.TruncateAt.MARQUEE);  
    setMarqueeRepeatLimit(-1);  
    setSingleLine(true);  
    setSelected(true);  
    setFocusable(false);
    setFocusableInTouchMode(false);

    setIncludeFontPadding(false);  

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {  
        setFallbackLineSpacing(false);  
    }  
}  

public void resetMarquee() {  
    marqueeEnabled = false;  
    setSelected(false);  

    post(() -> {  
        marqueeEnabled = true;  
        setSelected(true);  
    });
}  

@Override  
protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {  
    if (focused) {  
        super.onFocusChanged(true, direction, previouslyFocusedRect);  
    }  
}  

@Override  
public void onWindowFocusChanged(boolean hasWindowFocus) {  
    if (hasWindowFocus) {  
        super.onWindowFocusChanged(true);  
    }  
}  

@Override  
public boolean isFocused() {  
    return marqueeEnabled && super.isFocused();  
}

}

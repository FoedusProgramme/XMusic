package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.content.ContextCompat;
import com.xapps.media.xmusic.models.SquigglyProgress;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.utils.XUtils;

public class XSeekbar extends AppCompatSeekBar {

    public static final int STYLE_MATERIAL = 0;
    public static final int STYLE_EXPRESSIVE = 1;
    
    private Drawable materialThumb, expressiveThumb;

    private SquigglyProgress progressDrawable;

    public XSeekbar(Context context) {
        super(context);
        init(context);
    }

    public XSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public XSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        expressiveThumb = ContextCompat.getDrawable(context, R.drawable.seekbar_thumb);
        
        progressDrawable = new SquigglyProgress();
        progressDrawable.setWaveLength(100);
		progressDrawable.setLineAmplitude(8);
		progressDrawable.setPhaseSpeed(25);
		progressDrawable.setStrokeWidth(XUtils.convertToPx(context, 4f));
		progressDrawable.setTransitionEnabled(true);
		progressDrawable.setAnimate(true);
    }

    public void setColor(int color) {
        progressDrawable.setTint(color);
        setThumbTintList(ColorStateList.valueOf(color)); 
        setProgressTintList(ColorStateList.valueOf(color));
    }

    public void setStyle(int style) {
        if (materialThumb == null) materialThumb = getThumb();
        if (style == 0) {
            setThumb(materialThumb);
            setProgressDrawable(null);
            
        } else if (style == 1) {
            setThumb(expressiveThumb);
            setProgressDrawable(progressDrawable);
        } else {
            throw new IllegalArgumentException("Style int must be either 0 or 1");
        }
    }
}

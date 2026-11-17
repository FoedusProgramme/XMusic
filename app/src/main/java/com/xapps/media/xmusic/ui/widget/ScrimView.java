package com.xapps.media.xmusic.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.ColorInt;

public class ScrimView extends View {

    public static final int TOP_TO_BOTTOM = 0;
    public static final int BOTTOM_TO_TOP = 1;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int scrimColor;
    private float alphaMultiplier = 1f;

    private int gradientDirection = TOP_TO_BOTTOM;

    private LinearGradient gradient;

    private int currentHeight = 0;

    public ScrimView(Context c) { super(c); init(c, null); }
    public ScrimView(Context c, AttributeSet a) { super(c, a); init(c, a); }
    public ScrimView(Context c, AttributeSet a, int d) { super(c, a, d); init(c, a); }

    private void init(Context c, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray ta = c.obtainStyledAttributes(attrs, com.xapps.media.xmusic.R.styleable.ScrimView);
            scrimColor = ta.getColor(com.xapps.media.xmusic.R.styleable.ScrimView_scrimColor, 0xFF000000);
            ta.recycle();
        } else {
            scrimColor = 0xFF000000;
        }
        setClickable(false);
        setFocusable(false);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (currentHeight == 0) {
            currentHeight = h;
        }
        rebuildGradient(w, h);
    }

    private void rebuildGradient(int w, int h) {
        if (w <= 0 || h <= 0 || currentHeight <= 0) return;

        float startY;
        float endY;

        if (gradientDirection == TOP_TO_BOTTOM) {
            startY = 0;
            endY = currentHeight;
        } else {
            startY = h;
            endY = h - currentHeight;
        }

        int alpha = (int) (alphaMultiplier * ((scrimColor >>> 24) & 0xFF));
        int startColor = (alpha << 24) | (scrimColor & 0x00FFFFFF);

        gradient = new LinearGradient(
                0, startY,
                0, endY,
                startColor,
                0x00000000,
                Shader.TileMode.CLAMP
        );

        paint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (gradient == null) return;
        
        if (gradientDirection == TOP_TO_BOTTOM) {
            canvas.drawRect(0, 0, getWidth(), currentHeight, paint);
        } else {
            canvas.drawRect(0, getHeight() - currentHeight, getWidth(), getHeight(), paint);
        }
    }

    public void setScrimColor(@ColorInt int color) {
        if (this.scrimColor != color) {
            this.scrimColor = color;
            rebuildGradient(getWidth(), getHeight());
            invalidate();
        }
    }

    public void setGradientDirection(int direction) {
        if (this.gradientDirection != direction) {
            this.gradientDirection = direction;
            rebuildGradient(getWidth(), getHeight());
            invalidate();
        }
    }

    public void setAlphaMultiplier(float alpha) {
        alphaMultiplier = Math.max(0f, Math.min(1f, alpha));
        rebuildGradient(getWidth(), getHeight());
        invalidate();
    }

    public void setSolidPercentage(float p) {
        float solidPercentage = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    public void animateHeight(int targetHeight, long duration) {
        ValueAnimator anim = ValueAnimator.ofInt(currentHeight, targetHeight);
        anim.setDuration(duration);
        anim.setInterpolator(new DecelerateInterpolator());

        anim.addUpdateListener(a -> {
            currentHeight = (int) a.getAnimatedValue();
            rebuildGradient(getWidth(), getHeight());
            invalidate();
        });

        anim.start();
    }

    public void setInstantHeight(int h) {
        currentHeight = h;
        rebuildGradient(getWidth(), getHeight());
        invalidate();
    }
}

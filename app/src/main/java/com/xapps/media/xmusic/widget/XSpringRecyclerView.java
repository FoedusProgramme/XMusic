package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public class XSpringRecyclerView extends RecyclerView {

    private SpringAnimation springAnim;
    private float currentTranslation = 0f;
    private float lastY = 0f;
    private boolean isDraggingSpring = false;
    private static final float FRICTION_BASE = 0.4f;

    public interface OnSpringScrollListener {
        void onOffsetChanged(float offset);
        void onRefreshTriggered();
    }
    private OnSpringScrollListener listener;
    private boolean isRefreshing = false;
    private boolean thresholdReached = false;
    private static final float REFRESH_THRESHOLD = 200f;

    public XSpringRecyclerView(@NonNull Context context) {
        super(context);
        init();
    }

    public XSpringRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOverScrollMode(OVER_SCROLL_NEVER);
        springAnim = new SpringAnimation(this, DynamicAnimation.TRANSLATION_Y, 0f);
        springAnim.setSpring(new SpringForce(0f)
                .setStiffness(500f)
                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY));
        
        springAnim.addUpdateListener((animation, value, velocity) -> {
            currentTranslation = value;
            if (listener != null) listener.onOffsetChanged(value);
        });
    }

    public void setOnSpringScrollListener(OnSpringScrollListener l) {
        this.listener = l;
    }
    
    public void setRefreshing(boolean refreshing) {
        this.isRefreshing = refreshing;
        if (refreshing) {
            springAnim.animateToFinalPosition(REFRESH_THRESHOLD);
        } else {
            thresholdReached = false;
            springAnim.animateToFinalPosition(0f);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (springAnim.isRunning()) {
                    springAnim.cancel();
                    isDraggingSpring = Math.abs(currentTranslation) > 1f;
                } else {
                    isDraggingSpring = false;
                }
                lastY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                float currentY = ev.getRawY();
                float dy = currentY - lastY;
                lastY = currentY;

                if (isDraggingSpring) {
                    float previousTranslation = currentTranslation;
                    currentTranslation += dy * FRICTION_BASE;

                    if ((previousTranslation > 0 && currentTranslation <= 0) || 
                        (previousTranslation < 0 && currentTranslation >= 0)) {
                        currentTranslation = 0;
                        isDraggingSpring = false;
                        setTranslationY(0f);
                        if (listener != null) listener.onOffsetChanged(0f);
                        
                        MotionEvent downEvent = MotionEvent.obtain(ev);
                        downEvent.setAction(MotionEvent.ACTION_DOWN);
                        super.dispatchTouchEvent(downEvent);
                        downEvent.recycle();
                        return true;
                    }
                    
                    if (currentTranslation >= REFRESH_THRESHOLD && !thresholdReached) {
                        thresholdReached = true;
                    } else if (currentTranslation < REFRESH_THRESHOLD && thresholdReached) {
                        thresholdReached = false;
                    }
                    
                    setTranslationY(currentTranslation);
                    if (listener != null) listener.onOffsetChanged(currentTranslation);
                    return true; 
                } else {
                    if (dy > 0 && !canScrollVertically(-1) && currentTranslation >= 0) {
                        isDraggingSpring = true;
                        currentTranslation += dy * FRICTION_BASE;
                        setTranslationY(currentTranslation);
                        if (listener != null) listener.onOffsetChanged(currentTranslation);
                        
                        MotionEvent cancelEvent = MotionEvent.obtain(ev);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                        return true;
                    } else if (dy < 0 && !canScrollVertically(1) && currentTranslation <= 0) {
                        isDraggingSpring = true;
                        currentTranslation += dy * FRICTION_BASE;
                        setTranslationY(currentTranslation);
                        if (listener != null) listener.onOffsetChanged(currentTranslation);
                        
                        MotionEvent cancelEvent = MotionEvent.obtain(ev);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDraggingSpring || currentTranslation != 0) {
                    isDraggingSpring = false;
                    if (!isRefreshing && thresholdReached && currentTranslation > 0) {
                        setRefreshing(true);
                        if (listener != null) listener.onRefreshTriggered();
                    } else if (!isRefreshing) {
                        springAnim.setStartValue(currentTranslation);
                        springAnim.animateToFinalPosition(0f);
                    } else if (isRefreshing) {
                        springAnim.setStartValue(currentTranslation);
                        springAnim.animateToFinalPosition(REFRESH_THRESHOLD);
                    }
                }
                break;
        }
        
        if (isDraggingSpring) return true;
        return super.dispatchTouchEvent(ev);
    }
}

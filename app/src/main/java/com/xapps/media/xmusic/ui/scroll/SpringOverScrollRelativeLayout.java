package com.xapps.media.xmusic.ui.scroll;

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
import android.os.Build;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.google.android.material.appbar.AppBarLayout;

public class SpringOverScrollRelativeLayout extends RelativeLayout implements NestedScrollingParent3, NestedScrollingChild3, AppBarLayout.OnOffsetChangedListener {

    private static final float MAX_DRAG = 900f;
    private static final float PRE_THRESHOLD_FRICTION = 0.24f;
    private static final float POST_THRESHOLD_FRICTION = 0.54f;
    private static final float ENGAGE_RAW_DISTANCE = 480f;

    private static final float FLING_MULTIPLIER = 600f;
    private static final float MAX_FLING_VELOCITY = 5000f;

    private final NestedScrollingParentHelper parentHelper;
    private final NestedScrollingChildHelper childHelper;

    private SpringAnimation springAnim;
    private SpringAnimation catchUpSpring;

    private float verticalOffset = 0f;
    private float rawTouchDistance = 0f;
    private float baseTouchOffset = 0f;
    private float catchUpBonus = 0f;
    private boolean hasBrokenDetent = false;

    private View scrollTarget;

    private int appBarOffset = 0;
    private int totalAppBarRange = 0;

    public SpringOverScrollRelativeLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        parentHelper = new NestedScrollingParentHelper(this);
        childHelper = new NestedScrollingChildHelper(this);
        childHelper.setNestedScrollingEnabled(true);
        initSprings();
    }

    private void initSprings() {
        springAnim = new SpringAnimation(this, new FloatPropertyCompat<SpringOverScrollRelativeLayout>("offset") {
            @Override
            public float getValue(SpringOverScrollRelativeLayout o) {
                return verticalOffset;
            }

            @Override
            public void setValue(SpringOverScrollRelativeLayout o, float v) {
                verticalOffset = v;
                if (scrollTarget != null) {
                    scrollTarget.setTranslationY(verticalOffset);
                }
            }
        }, 0f);
        springAnim.setSpring(new SpringForce(0f)
                .setStiffness(340f)
                .setDampingRatio(0.82f));

        catchUpSpring = new SpringAnimation(this, new FloatPropertyCompat<SpringOverScrollRelativeLayout>("catchUp") {
            @Override
            public float getValue(SpringOverScrollRelativeLayout o) {
                return catchUpBonus;
            }

            @Override
            public void setValue(SpringOverScrollRelativeLayout o, float v) {
                catchUpBonus = v;
                if (!springAnim.isRunning()) {
                    verticalOffset = Math.max(-MAX_DRAG, Math.min(MAX_DRAG, baseTouchOffset + catchUpBonus));
                    if (scrollTarget != null) {
                        scrollTarget.setTranslationY(verticalOffset);
                    }
                }
            }
        }, 0f);
        catchUpSpring.setSpring(new SpringForce(0f)
                .setStiffness(680f)
                .setDampingRatio(0.56f));
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        this.appBarOffset = offset;
        this.totalAppBarRange = appBarLayout.getTotalScrollRange();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getParent() instanceof View) {
            AppBarLayout appBar = ((View) getParent()).findViewById(com.xapps.media.xmusic.R.id.appbar);
            if (appBar != null) {
                appBar.addOnOffsetChangedListener(this);
            }
        }
    }

    private float computeHeavyCurve(float rawDist) {
        float abs = Math.abs(rawDist);
        float sign = Math.signum(rawDist);
        float damped = MAX_DRAG * (float) (1.0 - Math.exp(-(abs * PRE_THRESHOLD_FRICTION) / MAX_DRAG));
        return sign * damped;
    }

    private float computeUnlockedCurve(float rawDist) {
        float abs = Math.abs(rawDist);
        float sign = Math.signum(rawDist);
        float damped = MAX_DRAG * (float) (1.0 - Math.exp(-(abs * POST_THRESHOLD_FRICTION) / MAX_DRAG));
        return sign * damped;
    }

    private void triggerHaptic(boolean reached) {
        try {
            int constant;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                constant = reached ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.CLOCK_TICK;
            } else {
                constant = HapticFeedbackConstants.CLOCK_TICK;
            }
            performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        } catch (Exception ignored) {}
    }

    private void applyOverScroll(float delta) {
        if (springAnim.isRunning()) springAnim.cancel();

        rawTouchDistance += delta;
        float absRaw = Math.abs(rawTouchDistance);

        if (!hasBrokenDetent) {
            if (absRaw >= ENGAGE_RAW_DISTANCE) {
                hasBrokenDetent = true;
                float heavyBeforeBreak = computeHeavyCurve(rawTouchDistance);
                float unlockedTarget = computeUnlockedCurve(rawTouchDistance);
                float absorbedDiff = unlockedTarget - heavyBeforeBreak;

                baseTouchOffset = unlockedTarget;
                catchUpBonus = -absorbedDiff;
                if (catchUpSpring.isRunning()) catchUpSpring.cancel();
                catchUpSpring.getSpring().setStiffness(680f).setDampingRatio(0.56f);
                catchUpSpring.setStartValue(catchUpBonus);
                catchUpSpring.setStartVelocity(Math.signum(rawTouchDistance) * 500f);
                catchUpSpring.animateToFinalPosition(0f);

                triggerHaptic(true);
            } else {
                baseTouchOffset = computeHeavyCurve(rawTouchDistance);
            }
        } else {
            baseTouchOffset = computeUnlockedCurve(rawTouchDistance);
        }

        verticalOffset = Math.max(-MAX_DRAG, Math.min(MAX_DRAG, baseTouchOffset + catchUpBonus));
        if (scrollTarget != null) {
            scrollTarget.setTranslationY(verticalOffset);
        }
    }

    private void triggerFlingSpring(float rawVelocity) {
        float velocity = Math.max(-MAX_FLING_VELOCITY, Math.min(MAX_FLING_VELOCITY, rawVelocity));
        if (springAnim.isRunning()) springAnim.cancel();
        if (catchUpSpring.isRunning()) catchUpSpring.cancel();
        catchUpBonus = 0f;
        rawTouchDistance = 0f;
        baseTouchOffset = 0f;
        hasBrokenDetent = false;
        springAnim.setStartVelocity(velocity);
        springAnim.setStartValue(verticalOffset);
        springAnim.animateToFinalPosition(0f);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (verticalOffset != 0f) {
            if ((verticalOffset > 0f && dy > 0) || (verticalOffset < 0f && dy < 0)) {
                if (type == ViewCompat.TYPE_TOUCH) {
                    applyOverScroll(-dy);
                    if ((verticalOffset <= 0f && dy > 0) || (verticalOffset >= 0f && dy < 0)) {
                        verticalOffset = 0f;
                        rawTouchDistance = 0f;
                        baseTouchOffset = 0f;
                        catchUpBonus = 0f;
                        hasBrokenDetent = false;
                        if (catchUpSpring.isRunning()) catchUpSpring.cancel();
                        if (scrollTarget != null) scrollTarget.setTranslationY(0f);
                    }
                    consumed[1] = dy;
                } else {
                    float oldOffset = verticalOffset;
                    if (verticalOffset > 0f) {
                        verticalOffset = Math.max(0f, verticalOffset - dy);
                    } else {
                        verticalOffset = Math.min(0f, verticalOffset - dy);
                    }
                    consumed[1] = (int) (oldOffset - verticalOffset);
                    if (scrollTarget != null) scrollTarget.setTranslationY(verticalOffset);
                }
            } else if ((verticalOffset > 0f && dy < 0) || (verticalOffset < 0f && dy > 0)) {
                if (type == ViewCompat.TYPE_TOUCH) {
                    applyOverScroll(-dy);
                } else {
                    ViewCompat.stopNestedScroll(target, ViewCompat.TYPE_NON_TOUCH);
                    triggerFlingSpring(-dy * FLING_MULTIPLIER);
                }
                consumed[1] = dy;
            }
        }

        if (dy != consumed[1]) {
            int[] pConsumed = new int[2];
            childHelper.dispatchNestedPreScroll(dx, dy - consumed[1], pConsumed, null, type);
            consumed[1] += pConsumed[1];
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxC, int dyC, int dxU, int dyU, int type, @NonNull int[] consumed) {
        int[] pConsumed = new int[2];
        childHelper.dispatchNestedScroll(dxC, dyC, dxU, dyU, null, type, pConsumed);
        consumed[1] += pConsumed[1];

        int rem = dyU - pConsumed[1];

        if (rem != 0) {
            boolean isAtTop = appBarOffset >= 0 && !target.canScrollVertically(-1);
            boolean isAtBottom = Math.abs(appBarOffset) >= totalAppBarRange && !target.canScrollVertically(1);

            if ((rem < 0 && isAtTop) || (rem > 0 && isAtBottom)) {
                if (type == ViewCompat.TYPE_TOUCH) {
                    applyOverScroll(-rem);
                } else {
                    ViewCompat.stopNestedScroll(target, ViewCompat.TYPE_NON_TOUCH);
                    triggerFlingSpring(-rem * FLING_MULTIPLIER);
                }
                consumed[1] += rem;
            }
        }
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        if (type == ViewCompat.TYPE_TOUCH) {
            if (springAnim.isRunning()) springAnim.cancel();
            if (catchUpSpring.isRunning()) catchUpSpring.cancel();
            if (verticalOffset == 0f) {
                rawTouchDistance = 0f;
                baseTouchOffset = 0f;
                catchUpBonus = 0f;
                hasBrokenDetent = false;
            }
        }
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type);
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, type);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        parentHelper.onStopNestedScroll(target, type);
        stopNestedScroll(type);
        if (verticalOffset != 0f && !springAnim.isRunning()) {
            if (catchUpSpring.isRunning()) catchUpSpring.cancel();
            catchUpBonus = 0f;
            rawTouchDistance = 0f;
            baseTouchOffset = 0f;
            hasBrokenDetent = false;
            springAnim.setStartVelocity(0f);
            springAnim.setStartValue(verticalOffset);
            springAnim.animateToFinalPosition(0f);
        }
    }

    @Override protected void onFinishInflate() { super.onFinishInflate(); if (getChildCount() > 0) scrollTarget = getChildAt(0); }
    @Override public void onNestedScroll(@NonNull View target, int dxC, int dyC, int dxU, int dyU, int type) { onNestedScroll(target, dxC, dyC, dxU, dyU, type, new int[2]); }
    @Override public void setNestedScrollingEnabled(boolean e) { childHelper.setNestedScrollingEnabled(e); }
    @Override public boolean isNestedScrollingEnabled() { return childHelper.isNestedScrollingEnabled(); }
    @Override public boolean startNestedScroll(int a, int t) { return childHelper.startNestedScroll(a, t); }
    @Override public void stopNestedScroll(int t) { childHelper.stopNestedScroll(t); }
    @Override public boolean hasNestedScrollingParent(int t) { return childHelper.hasNestedScrollingParent(t); }
    @Override public boolean dispatchNestedScroll(int dxC, int dyC, int dxU, int dyU, int[] o, int t) { return childHelper.dispatchNestedScroll(dxC, dyC, dxU, dyU, o, t); }
    @Override public void dispatchNestedScroll(int dxC, int dyC, int dxU, int dyU, int[] o, int t, int[] c) { childHelper.dispatchNestedScroll(dxC, dyC, dxU, dyU, o, t, c); }
    @Override public boolean dispatchNestedPreScroll(int dx, int dy, int[] c, int[] o, int t) { return childHelper.dispatchNestedPreScroll(dx, dy, c, o, t); }
}
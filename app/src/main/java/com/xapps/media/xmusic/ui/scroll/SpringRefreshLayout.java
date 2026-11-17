package com.xapps.media.xmusic.ui.scroll;

import com.xapps.media.xmusic.ui.widget.*;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public class SpringRefreshLayout extends RelativeLayout implements NestedScrollingParent3, NestedScrollingChild3 {

    private static final float MAX_DRAG = 540f;
    private static final float LOCKED_OFFSET = 250f;
    private static final float BUMP_TARGET_OFFSET = 300;

    private static final float FRICTION_BASE = 0.65f;
    private static final float FRICTION_BUMP_STRENGTH = 0.50f;
    private static final float BUMP_WIDTH = 65f;
    private static final float JUMP_BOOST = 95f;

    private static final float FLING_MULTIPLIER = 600f;
    private static final float MAX_FLING_VELOCITY = 5000f;

    private final NestedScrollingParentHelper parentHelper;
    private final NestedScrollingChildHelper childHelper;

    private SpringAnimation springAnim;
    private SpringAnimation indicatorScaleXAnim;
    private SpringAnimation indicatorScaleYAnim;
    private SpringAnimation indicatorAlphaAnim;

    private float verticalOffset = 0f;
    private float targetOffset = 0f;
    private boolean isRefreshing = false;
    private boolean isVisualThresholdReached = false;

    private OnRefreshListener refreshListener;
    private ExpressiveLoadingIndicator indicator;
    private View indicatorContainer;
    private View scrollTarget;

    public interface OnRefreshListener {
        void onRefresh();
    }

    public SpringRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        parentHelper = new NestedScrollingParentHelper(this);
        childHelper = new NestedScrollingChildHelper(this);
        childHelper.setNestedScrollingEnabled(true);
        initSprings();
    }

    private void initSprings() {
        springAnim = new SpringAnimation(this, new FloatPropertyCompat<SpringRefreshLayout>("offset") {
            @Override
            public float getValue(SpringRefreshLayout o) {
                return verticalOffset;
            }

            @Override
            public void setValue(SpringRefreshLayout o, float v) {
                verticalOffset = v;
                applyTransforms();
            }
        }, 0f);
        springAnim.setSpring(new SpringForce(0f)
                .setStiffness(500f)
                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY));
    }

    private void setupIndicatorSprings() {
        if (indicator == null) return;
        SpringForce scaleSpring = new SpringForce()
                .setStiffness(SpringForce.STIFFNESS_LOW)
                .setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
        indicatorScaleXAnim = new SpringAnimation(indicator, DynamicAnimation.SCALE_X, 1f).setSpring(scaleSpring);
        indicatorScaleYAnim = new SpringAnimation(indicator, DynamicAnimation.SCALE_Y, 1f).setSpring(scaleSpring);
        indicatorAlphaAnim = new SpringAnimation(indicator, DynamicAnimation.ALPHA, 1f)
                .setSpring(new SpringForce().setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY));
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
        } catch (Exception ignored) {
            try {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            } catch (Exception ignored2) {}
        }
    }

    private void applyTransforms() {
        if (scrollTarget == null) findScrollTarget();
        if (scrollTarget != null) {
            scrollTarget.setTranslationY(verticalOffset);
            if (verticalOffset < 0) {
                scrollTarget.setOverScrollMode(View.OVER_SCROLL_NEVER);
            }
        }

        if (indicatorContainer != null) {
            ViewGroup.LayoutParams lp = indicatorContainer.getLayoutParams();
            int newHeight = (int) Math.max(1, verticalOffset);
            if (lp.height != newHeight) {
                lp.height = newHeight;
                indicatorContainer.setLayoutParams(lp);
            }
        }

        if (indicator != null) {
            float h = indicator.getHeight() > 0 ? indicator.getHeight() : (64 * getResources().getDisplayMetrics().density);
            indicator.setTranslationY((verticalOffset / 2f) - (h / 2f));
            if (!isRefreshing) {
                indicator.setPullProgress(Math.min(1f, Math.max(0f, verticalOffset) / BUMP_TARGET_OFFSET));
            }
        }
    }

    private void triggerIndicatorIn() {
        if (indicator == null) return;
        indicator.setVisibility(VISIBLE);
        if (indicatorAlphaAnim.isRunning()) indicatorAlphaAnim.cancel();
        if (indicatorScaleXAnim.isRunning()) indicatorScaleXAnim.cancel();
        if (indicatorScaleYAnim.isRunning()) indicatorScaleYAnim.cancel();
        indicatorAlphaAnim.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
        indicatorAlphaAnim.setStartValue(0f).animateToFinalPosition(1f);
        indicatorScaleXAnim.setStartValue(0.7f).animateToFinalPosition(1f);
        indicatorScaleYAnim.setStartValue(0.7f).animateToFinalPosition(1f);
    }

    private void triggerIndicatorOut(boolean shouldCollapseAfter) {
        if (indicator == null) {
            if (shouldCollapseAfter) {
                targetOffset = 0f;
                springAnim.animateToFinalPosition(0f);
            }
            return;
        }
        indicatorAlphaAnim.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
        indicatorAlphaAnim.animateToFinalPosition(0f);
        indicatorScaleXAnim.animateToFinalPosition(0.7f);
        indicatorScaleYAnim.animateToFinalPosition(0.7f);
        indicatorAlphaAnim.addEndListener(new DynamicAnimation.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                if (!isVisualThresholdReached && !isRefreshing) indicator.setVisibility(INVISIBLE);
                if (shouldCollapseAfter) {
                    targetOffset = 0f;
                    springAnim.animateToFinalPosition(0f);
                }
                indicatorAlphaAnim.removeEndListener(this);
            }
        });
    }

    private void triggerFlingSpring(float rawVelocity) {
        if (isRefreshing) return;
        float velocity = Math.max(-MAX_FLING_VELOCITY, Math.min(MAX_FLING_VELOCITY, rawVelocity));
        if (springAnim.isRunning()) springAnim.cancel();
        targetOffset = 0f;
        springAnim.setStartVelocity(velocity);
        springAnim.setStartValue(verticalOffset);
        springAnim.animateToFinalPosition(0f);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxC, int dyC, int dxU, int dyU, int type, @NonNull int[] consumed) {
        if (scrollTarget == null || scrollTarget != target) scrollTarget = target;
        if (isRefreshing) return;

        int[] pConsumed = new int[2];
        childHelper.dispatchNestedScroll(dxC, dyC, dxU, dyU, null, type, pConsumed);
        consumed[1] += pConsumed[1];
        int rem = dyU - pConsumed[1];

        if (rem != 0) {
            if (type == ViewCompat.TYPE_TOUCH) {
                float threshold = BUMP_TARGET_OFFSET - JUMP_BOOST;

                float friction;
                if (targetOffset >= 0) {
                    float dist = targetOffset - threshold;
                    float bumpStrength = (float) Math.exp(-(dist * dist) / (2f * BUMP_WIDTH * BUMP_WIDTH));
                    friction = (targetOffset < threshold)
                            ? (FRICTION_BASE - (FRICTION_BUMP_STRENGTH * bumpStrength))
                            : (FRICTION_BASE * (1.0f - (float) Math.pow(Math.max(0f, targetOffset - BUMP_TARGET_OFFSET) / (MAX_DRAG - BUMP_TARGET_OFFSET), 2)));
                } else {
                    friction = FRICTION_BASE * (1.0f - Math.abs(targetOffset / MAX_DRAG));
                }

                targetOffset = Math.min(MAX_DRAG, Math.max(-MAX_DRAG, targetOffset - (rem * friction)));

                if (rem < 0) {
                    if (targetOffset >= threshold && !isVisualThresholdReached) {
                        isVisualThresholdReached = true;
                        targetOffset = BUMP_TARGET_OFFSET;
                        triggerHaptic(true);
                        triggerIndicatorIn();
                    }
                } else {
                    if (targetOffset > 0 && isVisualThresholdReached && targetOffset < BUMP_TARGET_OFFSET - JUMP_BOOST) {
                        isVisualThresholdReached = false;
                        targetOffset = Math.max(0f, targetOffset - JUMP_BOOST);
                        triggerHaptic(false);
                        triggerIndicatorOut(false);
                    }
                }

                springAnim.animateToFinalPosition(targetOffset);
            } else {
                ViewCompat.stopNestedScroll(target, ViewCompat.TYPE_NON_TOUCH);
                triggerFlingSpring(-rem * FLING_MULTIPLIER);
            }
            consumed[1] += rem;
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (this.isRefreshing == refreshing) return;
        this.isRefreshing = refreshing;
        if (refreshing) {
            targetOffset = LOCKED_OFFSET;
            springAnim.animateToFinalPosition(LOCKED_OFFSET);
            if (indicator != null) {
                indicator.setVisibility(VISIBLE);
                indicator.setAlpha(1f);
                indicator.setScaleX(1f);
                indicator.setScaleY(1f);
                indicator.startRefreshing();
            }
        } else {
            isVisualThresholdReached = false;
            if (indicator != null) {
                indicator.stopRefreshing();
                triggerIndicatorOut(true);
            } else {
                targetOffset = 0f;
                springAnim.animateToFinalPosition(0f);
            }
        }
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public void setScrollTarget(View target) {
        this.scrollTarget = target;
    }

    private void findScrollTarget() {
        if (scrollTarget != null) return;
        scrollTarget = findViewById(com.xapps.media.xmusic.R.id.songsList);
        if (scrollTarget == null) {
            scrollTarget = findViewById(com.xapps.media.xmusic.R.id.artistsList);
        }
        if (scrollTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child != indicatorContainer) {
                    scrollTarget = child;
                    break;
                }
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        indicatorContainer = findViewById(com.xapps.media.xmusic.R.id.indicatorContainer);
        findScrollTarget();
        if (indicatorContainer instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) indicatorContainer;
            for (int i = 0; i < group.getChildCount(); i++) {
                View v = group.getChildAt(i);
                if (v instanceof ExpressiveLoadingIndicator) {
                    indicator = (ExpressiveLoadingIndicator) v;
                    indicator.bringToFront();
                    setupIndicatorSprings();
                    break;
                }
            }
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxC, int dyC, int dxU, int dyU, int type) {
        onNestedScroll(target, dxC, dyC, dxU, dyU, type, new int[2]);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        parentHelper.onStopNestedScroll(target, type);
        stopNestedScroll(type);
        if (type == ViewCompat.TYPE_TOUCH) {
            if (!isRefreshing && isVisualThresholdReached) {
                setRefreshing(true);
                if (refreshListener != null) refreshListener.onRefresh();
            } else if (!isRefreshing && (targetOffset != 0f || verticalOffset != 0f)) {
                isVisualThresholdReached = false;
                targetOffset = 0f;
                springAnim.animateToFinalPosition(0f);
                triggerIndicatorOut(false);
            }
        } else if (type == ViewCompat.TYPE_NON_TOUCH) {
            if (!isRefreshing && verticalOffset != 0f && !springAnim.isRunning()) {
                targetOffset = 0f;
                springAnim.animateToFinalPosition(0f);
            }
        }
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        if (scrollTarget == null || scrollTarget != target) scrollTarget = target;
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0 || startNestedScroll(axes, type);
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type);
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, type);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (scrollTarget == null || scrollTarget != target) scrollTarget = target;
        if (isRefreshing) {
            consumed[1] = dy;
            return;
        }

        if (targetOffset > 0 && dy > 0) {
            float oldOffset = targetOffset;
            targetOffset = Math.max(0f, targetOffset - dy);
            if (oldOffset >= (BUMP_TARGET_OFFSET - JUMP_BOOST) && targetOffset < (BUMP_TARGET_OFFSET - JUMP_BOOST) && isVisualThresholdReached) {
                isVisualThresholdReached = false;
                targetOffset = Math.max(0f, targetOffset - JUMP_BOOST);
                triggerHaptic(false);
                triggerIndicatorOut(false);
            }
            springAnim.animateToFinalPosition(targetOffset);
            consumed[1] = (int) (oldOffset - targetOffset);
        } else if (targetOffset < 0 && dy < 0) {
            float oldOffset = targetOffset;
            targetOffset = Math.min(0f, targetOffset - dy);
            springAnim.animateToFinalPosition(targetOffset);
            consumed[1] = (int) (oldOffset - targetOffset);
        }

        if (dy != consumed[1]) {
            int selfConsumed = consumed[1];
            int[] parentConsumed = new int[2];
            childHelper.dispatchNestedPreScroll(dx, dy - selfConsumed, parentConsumed, null, type);
            consumed[1] = selfConsumed + parentConsumed[1];
            consumed[0] += parentConsumed[0];
        }
    }

    @Override public void setNestedScrollingEnabled(boolean e) { childHelper.setNestedScrollingEnabled(e); }
    @Override public boolean isNestedScrollingEnabled() { return childHelper.isNestedScrollingEnabled(); }
    @Override public boolean startNestedScroll(int a, int t) { return childHelper.startNestedScroll(a, t); }
    @Override public void stopNestedScroll(int t) { childHelper.stopNestedScroll(t); }
    @Override public boolean hasNestedScrollingParent(int t) { return childHelper.hasNestedScrollingParent(t); }
    @Override public boolean dispatchNestedScroll(int dxC, int dyC, int dxU, int dyU, int[] o, int t) { return childHelper.dispatchNestedScroll(dxC, dyC, dxU, dyU, o, t); }
    @Override public void dispatchNestedScroll(int dxC, int dyC, int dxU, int dyU, int[] o, int t, int[] c) { childHelper.dispatchNestedScroll(dxC, dyC, dxU, dyU, o, t, c); }
    @Override public boolean dispatchNestedPreScroll(int dx, int dy, int[] c, int[] o, int t) { return childHelper.dispatchNestedPreScroll(dx, dy, c, o, t); }
    public void setOnRefreshListener(OnRefreshListener l) { this.refreshListener = l; }
}
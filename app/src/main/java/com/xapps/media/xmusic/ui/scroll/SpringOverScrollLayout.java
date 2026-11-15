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
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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

public class SpringOverScrollLayout extends RelativeLayout implements NestedScrollingParent3, NestedScrollingChild3 {

    private static final float MAX_DRAG = 960f;
    private static final float PRE_THRESHOLD_FRICTION = 0.22f;
    private static final float POST_THRESHOLD_FRICTION = 0.56f;
    private static final float ENGAGE_RAW_DISTANCE = 440f;

    private static final float FLING_MULTIPLIER = 600f;
    private static final float MAX_FLING_VELOCITY = 5000f;

    private final NestedScrollingParentHelper parentHelper;
    private final NestedScrollingChildHelper childHelper;

    private SpringAnimation springAnim;
    private SpringAnimation catchUpSpring;
    private SpringAnimation indicatorScaleXAnim;
    private SpringAnimation indicatorScaleYAnim;
    private SpringAnimation indicatorAlphaAnim;

    private float verticalOffset = 0f;
    private float rawTouchDistance = 0f;
    private float baseTouchOffset = 0f;
    private float catchUpBonus = 0f;

    private boolean isRefreshing = false;
    private boolean hasBrokenDetent = false;
    private boolean isIntentionalPull = false;
    private boolean isCollapsingIndicator = false;

    private OnRefreshListener refreshListener;
    private CustomLoadingIndicator indicator;
    private View indicatorContainer;
    private View scrollTarget;

    public interface OnRefreshListener {
        void onRefresh();
    }

    public SpringOverScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        parentHelper = new NestedScrollingParentHelper(this);
        childHelper = new NestedScrollingChildHelper(this);
        childHelper.setNestedScrollingEnabled(true);
        setClipChildren(false);
        setClipToPadding(false);
        initSprings();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float getIndicatorSizePx() {
        if (indicator != null && indicator.getHeight() > 0) {
            return indicator.getHeight();
        }
        return dpToPx(56f);
    }

    private float getExtraTopMarginPx() {
        return dpToPx(16f);
    }

    private float getLockedOffset() {
        return getIndicatorSizePx() + dpToPx(56f);
    }

    private void initSprings() {
        springAnim = new SpringAnimation(this, new FloatPropertyCompat<SpringOverScrollLayout>("offset") {
            @Override
            public float getValue(SpringOverScrollLayout o) {
                return verticalOffset;
            }

            @Override
            public void setValue(SpringOverScrollLayout o, float v) {
                verticalOffset = v;
                applyTransforms();
            }
        }, 0f);
        springAnim.setSpring(new SpringForce(0f)
                .setStiffness(340f)
                .setDampingRatio(0.82f));

        catchUpSpring = new SpringAnimation(this, new FloatPropertyCompat<SpringOverScrollLayout>("catchUp") {
            @Override
            public float getValue(SpringOverScrollLayout o) {
                return catchUpBonus;
            }

            @Override
            public void setValue(SpringOverScrollLayout o, float v) {
                catchUpBonus = v;
                if (!springAnim.isRunning() && !isRefreshing) {
                    verticalOffset = Math.max(-MAX_DRAG, Math.min(MAX_DRAG, baseTouchOffset + catchUpBonus));
                    applyTransforms();
                }
            }
        }, 0f);
        catchUpSpring.setSpring(new SpringForce(0f)
                .setStiffness(680f)
                .setDampingRatio(0.56f));
    }

    private void setupIndicatorSprings() {
        if (indicator == null) return;
        SpringForce scaleSpringX = new SpringForce(1f)
                .setStiffness(680f)
                .setDampingRatio(0.56f);
        SpringForce scaleSpringY = new SpringForce(1f)
                .setStiffness(680f)
                .setDampingRatio(0.56f);
        indicatorScaleXAnim = new SpringAnimation(indicator, DynamicAnimation.SCALE_X, 1f).setSpring(scaleSpringX);
        indicatorScaleYAnim = new SpringAnimation(indicator, DynamicAnimation.SCALE_Y, 1f).setSpring(scaleSpringY);
        indicatorAlphaAnim = new SpringAnimation(indicator, DynamicAnimation.ALPHA, 1f)
                .setSpring(new SpringForce(1f)
                        .setStiffness(420f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY));
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
        float targetAtEngage = getLockedOffset();
        float baseAtEngage = MAX_DRAG * (float) (1.0 - Math.exp(-(ENGAGE_RAW_DISTANCE * POST_THRESHOLD_FRICTION) / MAX_DRAG));
        float scaleFactor = targetAtEngage / baseAtEngage;
        float damped = MAX_DRAG * (float) (1.0 - Math.exp(-(abs * POST_THRESHOLD_FRICTION * scaleFactor) / MAX_DRAG));
        return sign * damped;
    }

    private void applyOverScroll(float delta) {
        if (isRefreshing) return;
        if (springAnim.isRunning()) springAnim.cancel();

        if (isCollapsingIndicator) {
            isCollapsingIndicator = false;
            if (indicatorAlphaAnim != null && indicatorAlphaAnim.isRunning()) indicatorAlphaAnim.cancel();
            if (indicatorScaleXAnim != null && indicatorScaleXAnim.isRunning()) indicatorScaleXAnim.cancel();
            if (indicatorScaleYAnim != null && indicatorScaleYAnim.isRunning()) indicatorScaleYAnim.cancel();
        }

        isIntentionalPull = true;
        rawTouchDistance += delta;

        if (!hasBrokenDetent) {
            if (rawTouchDistance >= ENGAGE_RAW_DISTANCE) {
                hasBrokenDetent = true;
                float heavyBeforeBreak = computeHeavyCurve(rawTouchDistance);
                float unlockedTarget = computeUnlockedCurve(rawTouchDistance);
                float absorbedDiff = unlockedTarget - heavyBeforeBreak;

                baseTouchOffset = unlockedTarget;
                catchUpBonus = -absorbedDiff;
                if (catchUpSpring.isRunning()) catchUpSpring.cancel();
                catchUpSpring.getSpring().setStiffness(680f).setDampingRatio(0.56f);
                catchUpSpring.setStartValue(catchUpBonus);
                catchUpSpring.setStartVelocity(520f);
                catchUpSpring.animateToFinalPosition(0f);

                if (indicator != null) {
                    indicator.onDetentBroken();
                }
                triggerHaptic(true);
                animateIndicatorPopSynced();
            } else if (rawTouchDistance >= 0f) {
                baseTouchOffset = computeHeavyCurve(rawTouchDistance);
            } else {
                float absRaw = Math.abs(rawTouchDistance);
                float ratio = Math.min(1f, Math.abs(verticalOffset) / MAX_DRAG);
                float bottomFriction = POST_THRESHOLD_FRICTION * (1f - 0.65f * ratio);
                baseTouchOffset = Math.max(-MAX_DRAG, baseTouchOffset + delta * bottomFriction);
                rawTouchDistance = -absRaw;
            }
        } else {
            baseTouchOffset = computeUnlockedCurve(Math.max(0f, rawTouchDistance));
        }

        verticalOffset = Math.max(-MAX_DRAG, Math.min(MAX_DRAG, baseTouchOffset + catchUpBonus));
        applyTransforms();
    }

    private float computePullScale(float rawDist) {
        float progress = Math.min(1f, Math.max(0f, rawDist) / ENGAGE_RAW_DISTANCE);
        float eased = 1f - (1f - progress) * (1f - progress);
        return 0.40f + (0.45f * eased);
    }

    private void animateIndicatorPopSynced() {
        if (indicator == null || indicatorScaleXAnim == null || indicatorScaleYAnim == null) return;
        if (indicatorScaleXAnim.isRunning()) indicatorScaleXAnim.cancel();
        if (indicatorScaleYAnim.isRunning()) indicatorScaleYAnim.cancel();

        if (indicatorAlphaAnim != null && indicatorAlphaAnim.isRunning()) {
            indicatorAlphaAnim.cancel();
        }
        indicator.setAlpha(1f);

        float startScale = Math.max(0.82f, indicator.getScaleX());
        indicator.setScaleX(startScale);
        indicator.setScaleY(startScale);

        indicatorScaleXAnim.getSpring().setStiffness(680f).setDampingRatio(0.54f);
        indicatorScaleYAnim.getSpring().setStiffness(680f).setDampingRatio(0.54f);
        indicatorScaleXAnim.setStartValue(startScale);
        indicatorScaleYAnim.setStartValue(startScale);
        indicatorScaleXAnim.setStartVelocity(4.8f).animateToFinalPosition(1.0f);
        indicatorScaleYAnim.setStartVelocity(4.8f).animateToFinalPosition(1.0f);
    }

    private void animateIndicatorScaleTo(float targetScale, float startVel) {
        if (indicator == null || indicatorScaleXAnim == null || indicatorScaleYAnim == null) return;
        indicatorScaleXAnim.getSpring().setStiffness(420f).setDampingRatio(0.75f);
        indicatorScaleYAnim.getSpring().setStiffness(420f).setDampingRatio(0.75f);
        if (startVel != 0f) {
            indicatorScaleXAnim.setStartVelocity(startVel);
            indicatorScaleYAnim.setStartVelocity(startVel);
        }
        indicatorScaleXAnim.animateToFinalPosition(targetScale);
        indicatorScaleYAnim.animateToFinalPosition(targetScale);
    }

    private boolean shouldTriggerRefreshOnRelease() {
        if (!hasBrokenDetent) return false;
        float cancelThreshold = getLockedOffset() * 0.45f;
        return baseTouchOffset >= cancelThreshold;
    }

    private void triggerFlingSpring(float rawVelocity) {
        if (isRefreshing) return;

        if (isIntentionalPull && (verticalOffset > 0f || hasBrokenDetent)) {
            if (shouldTriggerRefreshOnRelease()) {
                setRefreshing(true);
                if (refreshListener != null) {
                    refreshListener.onRefresh();
                }
                return;
            } else {
                hasBrokenDetent = false;
                triggerIndicatorOut(true);
                return;
            }
        }

        isIntentionalPull = false;
        isCollapsingIndicator = false;
        hasBrokenDetent = false;
        if (indicator != null && indicator.getVisibility() != INVISIBLE) {
            indicator.setVisibility(INVISIBLE);
        }
        float velocity = Math.max(-MAX_FLING_VELOCITY, Math.min(MAX_FLING_VELOCITY, rawVelocity));
        if (springAnim.isRunning()) springAnim.cancel();
        if (catchUpSpring.isRunning()) catchUpSpring.cancel();
        catchUpBonus = 0f;
        rawTouchDistance = 0f;
        baseTouchOffset = 0f;
        springAnim.getSpring().setStiffness(340f).setDampingRatio(0.80f);
        springAnim.setStartVelocity(velocity);
        springAnim.setStartValue(verticalOffset);
        springAnim.animateToFinalPosition(0f);
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

        if (indicator != null) {
            float h = getIndicatorSizePx();
            float extraTop = getExtraTopMarginPx();
            float desiredTop = ((verticalOffset - h) * 0.5f) + (extraTop * 0.5f);
            float minSafeTop = dpToPx(12f);
            float finalTop = Math.max(minSafeTop, desiredTop);
            indicator.setTranslationY(finalTop);

            if (!isRefreshing) {
                if (isCollapsingIndicator) {
                    return;
                }

                if (!isIntentionalPull) {
                    if (indicator.getVisibility() != INVISIBLE) {
                        indicator.setVisibility(INVISIBLE);
                    }
                    return;
                }

                if (verticalOffset <= 1f && !catchUpSpring.isRunning() && !hasBrokenDetent) {
                    if (indicator.getVisibility() != INVISIBLE) {
                        indicator.setVisibility(INVISIBLE);
                    }
                    return;
                }

                if (indicator.getVisibility() != VISIBLE) {
                    indicator.setVisibility(VISIBLE);
                }

                float preBreakMaxOffset = computeHeavyCurve(ENGAGE_RAW_DISTANCE);
                float lockedOffset = getLockedOffset();

                float rawRatio = Math.max(0f, rawTouchDistance) / ENGAGE_RAW_DISTANCE;
                indicator.setPullProgress(rawRatio);

                if (!hasBrokenDetent) {
                    float alphaProgress = Math.min(1f, verticalOffset / (preBreakMaxOffset * 0.60f));
                    float smoothAlpha = alphaProgress * alphaProgress * (3f - 2f * alphaProgress);
                    if (indicatorAlphaAnim != null && indicatorAlphaAnim.isRunning()) {
                        indicatorAlphaAnim.cancel();
                    }
                    indicator.setAlpha(smoothAlpha);

                    float scale = computePullScale(rawTouchDistance);
                    if (indicatorScaleXAnim != null && !indicatorScaleXAnim.isRunning()) {
                        indicator.setScaleX(scale);
                        indicator.setScaleY(scale);
                    } else if (indicatorScaleXAnim != null) {
                        indicatorScaleXAnim.animateToFinalPosition(scale);
                        indicatorScaleYAnim.animateToFinalPosition(scale);
                    }
                } else {
                    float fadeZone = dpToPx(48f);
                    if (baseTouchOffset < fadeZone) {
                        float returnAlpha = Math.max(0f, baseTouchOffset / fadeZone);
                        indicator.setAlpha(returnAlpha);
                        float returnScale = 0.45f + (0.55f * returnAlpha);
                        if (indicatorScaleXAnim != null && indicatorScaleYAnim != null) {
                            indicatorScaleXAnim.animateToFinalPosition(returnScale);
                            indicatorScaleYAnim.animateToFinalPosition(returnScale);
                        }
                    } else {
                        indicator.setAlpha(1f);
                        float extraStretch = Math.max(0f, verticalOffset - lockedOffset) / (MAX_DRAG - lockedOffset);
                        float targetScale = 1.0f + (0.08f * Math.min(1f, extraStretch));
                        if (indicatorScaleXAnim != null && indicatorScaleYAnim != null && !catchUpSpring.isRunning()) {
                            indicatorScaleXAnim.animateToFinalPosition(targetScale);
                            indicatorScaleYAnim.animateToFinalPosition(targetScale);
                        }
                    }
                }
            }
        }
    }

    private void triggerIndicatorOut(boolean shouldCollapseAfter) {
        if (indicator == null) {
            if (shouldCollapseAfter) {
                collapseToZero();
            }
            return;
        }

        isCollapsingIndicator = true;

        if (indicatorAlphaAnim != null) {
            if (indicatorAlphaAnim.isRunning()) indicatorAlphaAnim.cancel();
            indicatorAlphaAnim.getSpring().setStiffness(340f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
            indicatorAlphaAnim.setStartValue(indicator.getAlpha());
            indicatorAlphaAnim.animateToFinalPosition(0f);
        }
        if (indicatorScaleXAnim != null) {
            if (indicatorScaleXAnim.isRunning()) indicatorScaleXAnim.cancel();
            indicatorScaleXAnim.getSpring().setStiffness(360f).setDampingRatio(0.82f);
            indicatorScaleXAnim.setStartValue(indicator.getScaleX());
            indicatorScaleXAnim.animateToFinalPosition(0.35f);
        }
        if (indicatorScaleYAnim != null) {
            if (indicatorScaleYAnim.isRunning()) indicatorScaleYAnim.cancel();
            indicatorScaleYAnim.getSpring().setStiffness(360f).setDampingRatio(0.82f);
            indicatorScaleYAnim.setStartValue(indicator.getScaleY());
            indicatorScaleYAnim.animateToFinalPosition(0.35f);
        }

        if (indicatorAlphaAnim != null) {
            indicatorAlphaAnim.addEndListener(new DynamicAnimation.OnAnimationEndListener() {
                @Override
                public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                    if (!canceled && !isRefreshing && indicator != null) {
                        indicator.setVisibility(INVISIBLE);
                        isIntentionalPull = false;
                        isCollapsingIndicator = false;
                    }
                    indicatorAlphaAnim.removeEndListener(this);
                }
            });
        }

        if (shouldCollapseAfter) {
            collapseToZero();
        }
    }

    private void collapseToZero() {
        if (springAnim.isRunning()) springAnim.cancel();
        if (catchUpSpring.isRunning()) catchUpSpring.cancel();
        catchUpBonus = 0f;
        rawTouchDistance = 0f;
        baseTouchOffset = 0f;
        hasBrokenDetent = false;
        springAnim.getSpring().setStiffness(320f).setDampingRatio(0.84f);
        springAnim.setStartVelocity(0f);
        springAnim.setStartValue(verticalOffset);
        springAnim.animateToFinalPosition(0f);
    }

    public void setRefreshing(boolean refreshing) {
        if (this.isRefreshing == refreshing) return;
        this.isRefreshing = refreshing;
        if (refreshing) {
            isCollapsingIndicator = false;
            float currentEffectiveOffset = Math.max(verticalOffset, baseTouchOffset + catchUpBonus);
            if (springAnim.isRunning()) springAnim.cancel();
            if (catchUpSpring.isRunning()) catchUpSpring.cancel();
            catchUpBonus = 0f;
            rawTouchDistance = 0f;
            float lockedTarget = getLockedOffset();
            baseTouchOffset = lockedTarget;
            verticalOffset = currentEffectiveOffset;

            springAnim.getSpring().setStiffness(380f).setDampingRatio(0.76f);
            springAnim.setStartVelocity(0f);
            springAnim.setStartValue(verticalOffset);
            springAnim.animateToFinalPosition(lockedTarget);

            if (indicator != null) {
                indicator.setVisibility(VISIBLE);
                indicator.setAlpha(1f);
                animateIndicatorScaleTo(1f, 0f);
                indicator.startRefreshing();
            }
        } else {
            hasBrokenDetent = false;
            if (indicator != null) {
                indicator.stopRefreshing();
                triggerIndicatorOut(true);
            } else {
                isIntentionalPull = false;
                isCollapsingIndicator = false;
                collapseToZero();
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
            group.setClipChildren(false);
            group.setClipToPadding(false);
            ViewGroup.LayoutParams containerLp = group.getLayoutParams();
            if (containerLp != null) {
                containerLp.height = (int) MAX_DRAG;
                group.setLayoutParams(containerLp);
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                View v = group.getChildAt(i);
                if (v instanceof CustomLoadingIndicator) {
                    indicator = (CustomLoadingIndicator) v;
                    ViewGroup.LayoutParams indLp = indicator.getLayoutParams();
                    if (indLp instanceof RelativeLayout.LayoutParams) {
                        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) indLp;
                        rlp.removeRule(RelativeLayout.CENTER_IN_PARENT);
                        rlp.removeRule(RelativeLayout.CENTER_VERTICAL);
                        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        rlp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        rlp.topMargin = 0;
                        indicator.setLayoutParams(rlp);
                    } else if (indLp instanceof FrameLayout.LayoutParams) {
                        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) indLp;
                        flp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                        flp.topMargin = 0;
                        indicator.setLayoutParams(flp);
                    }
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
            if (!isRefreshing) {
                if (shouldTriggerRefreshOnRelease()) {
                    setRefreshing(true);
                    if (refreshListener != null) {
                        refreshListener.onRefresh();
                    }
                } else if (verticalOffset != 0f || isIntentionalPull) {
                    hasBrokenDetent = false;
                    triggerIndicatorOut(true);
                }
            }
        } else if (type == ViewCompat.TYPE_NON_TOUCH) {
            if (verticalOffset != 0f && !springAnim.isRunning() && !isRefreshing && !isCollapsingIndicator) {
                if (isIntentionalPull && verticalOffset > 0f) {
                    if (shouldTriggerRefreshOnRelease()) {
                        setRefreshing(true);
                        if (refreshListener != null) {
                            refreshListener.onRefresh();
                        }
                    } else {
                        triggerIndicatorOut(true);
                    }
                } else {
                    isIntentionalPull = false;
                    collapseToZero();
                }
            }
        }
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        if (scrollTarget == null || scrollTarget != target) scrollTarget = target;
        if (type == ViewCompat.TYPE_TOUCH) {
            if (springAnim.isRunning()) springAnim.cancel();
            if (catchUpSpring.isRunning()) catchUpSpring.cancel();
            isCollapsingIndicator = false;
            if (!isRefreshing && verticalOffset == 0f) {
                rawTouchDistance = 0f;
                baseTouchOffset = 0f;
                catchUpBonus = 0f;
                hasBrokenDetent = false;
            }
        }
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

        if (verticalOffset != 0f || hasBrokenDetent) {
            if ((verticalOffset > 0f && dy > 0) || (verticalOffset < 0f && dy < 0)) {
                if (type == ViewCompat.TYPE_TOUCH) {
                    applyOverScroll(-dy);
                    if ((baseTouchOffset <= 0f && dy > 0) || (baseTouchOffset >= 0f && dy < 0)) {
                        verticalOffset = 0f;
                        rawTouchDistance = 0f;
                        baseTouchOffset = 0f;
                        catchUpBonus = 0f;
                        hasBrokenDetent = false;
                        isIntentionalPull = false;
                        isCollapsingIndicator = false;
                        if (catchUpSpring.isRunning()) catchUpSpring.cancel();
                        applyTransforms();
                    }
                    consumed[1] = dy;
                } else {
                    if (isIntentionalPull) {
                        consumed[1] = dy;
                        return;
                    }
                    float oldOffset = verticalOffset;
                    if (verticalOffset > 0f) {
                        verticalOffset = Math.max(0f, verticalOffset - dy);
                    } else {
                        verticalOffset = Math.min(0f, verticalOffset - dy);
                    }
                    consumed[1] = (int) (oldOffset - verticalOffset);
                    applyTransforms();
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
            int selfConsumed = consumed[1];
            int[] parentConsumed = new int[2];
            childHelper.dispatchNestedPreScroll(dx, dy - selfConsumed, parentConsumed, null, type);
            consumed[1] = selfConsumed + parentConsumed[1];
            consumed[0] += parentConsumed[0];
        }
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
                applyOverScroll(-rem);
            } else {
                ViewCompat.stopNestedScroll(target, ViewCompat.TYPE_NON_TOUCH);
                triggerFlingSpring(-rem * FLING_MULTIPLIER);
            }
            consumed[1] += rem;
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
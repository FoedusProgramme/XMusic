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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.PathInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xapps.media.xmusic.callback.PlaybackControlListener;
import com.xapps.media.xmusic.models.LyricItem;
import com.xapps.media.xmusic.models.LyricLine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XLyricsContainerView extends ScrollingView2 {

    private final List<LyricItemDelegate> lineDelegates = new ArrayList<>();
    private PlaybackControlListener listener;

    private int targetIndex = -1;
    private int activeBlurIndex = -1;

    private ValueAnimator staticScrollAnimator;
    private float preciseScrollY = -1f;
    private float staticScrollSourceY = -1f;
    private float staticScrollBaseTargetY = -1f;
    private float staticScrollTargetY = -1f;
    private float staticScrollShiftStartFraction = 0f;
    private float staticScrollShiftStartOffset = 0f;
    private float staticScrollShiftTargetOffset = 0f;
    private long lastFrameTime = 0;
    private int lastProgressMs = -1;
    private long manualSeekLockUntil = 0L;
    private int lockedTargetIndex = -1;
    private long lockedSeekTimestamp = -1L;

    private boolean isAutoScrollPaused = false;
    private boolean isPhysicsScrolling = false;
    private boolean isActuallyScrolling = false;
    private boolean isInteractive = true;

    private boolean useStaticScroll = false;
    private boolean userStaticScroll = false;
    private boolean enableSparkles = false;
    private boolean lyricAnticipation = false;
    private boolean enableBlurs = false;

    private BlurMaskFilter[] blurFilters;

    private float touchStartY = 0f;
    private int touchSlop;
    private LyricItemDelegate pressedDelegate;
    private int pressedPart = 0;
    private final Runnable recoveryRunnable = () -> {
        isAutoScrollPaused = false;
        if (targetIndex >= 0 && targetIndex < lineDelegates.size()) {
            float density = getResources().getDisplayMetrics().density;
            int screenHeight = getHeight();
            if (screenHeight > 0) {
                float activeMainCenterY = getTargetCenterY(targetIndex, screenHeight, density);
                float focalY = screenHeight * 0.28f;
                float desiredScrollY = Math.max(0, Math.min(activeMainCenterY - focalY, getScrollRange()));
                float currentScrollY = (preciseScrollY >= 0f) ? preciseScrollY : getScrollY();
                float distance = Math.abs(desiredScrollY - currentScrollY);
                if (distance > 5f * density) {
                    int currentLine = findClosestLineIndexToScroll(currentScrollY, screenHeight, density);
                    int lineDistance = Math.abs(targetIndex - currentLine);

                    if (useStaticScroll || lineDistance > 6) {
                        performSmoothStaticScroll(desiredScrollY);
                    } else {
                        performStaggeredScroll(targetIndex, false);
                    }
                }
            }
        }
    };

    private int currentColor;
    private String currentFontConfig;
    private float currentTextSizeDp = -1f;
    private boolean useSystemFont = false;
    private int lastLyricsHash;

    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public XLyricsContainerView(@NonNull Context context) {
        this(context, null);
    }

    public XLyricsContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipToOutline(false);
        initBlurFilters();
    }

    private void initBlurFilters() {
        blurFilters = new BlurMaskFilter[5];
        for (int i = 0; i < 5; i++) {
            blurFilters[i] = new BlurMaskFilter((i + 1) * 4f, BlurMaskFilter.Blur.NORMAL);
        }
    }

    public void setEnableBlurs(boolean enable) {
        this.enableBlurs = enable;
        if (!enable) {
            for (LyricItemDelegate delegate : lineDelegates) {
                delegate.setTargetBlurRadius(0f);
                delegate.setBlurFilter(null);
            }
        }
        invalidate();
    }

    public void setLyricAnticipation(boolean anticipate) {
        this.lyricAnticipation = anticipate;
    }

    public void setStaticScroll(boolean staticScroll) {
        this.useStaticScroll = staticScroll;
        if (staticScroll) {
            for (LyricItemDelegate delegate : lineDelegates) {
                delegate.staggerY = 0f;
                delegate.staggerVy = 0f;
                delegate.staggerDelay = 0f;
            }
            invalidate();
        }
    }

    public void setUserStaticScroll(boolean userStaticScroll) {
        this.userStaticScroll = userStaticScroll;
    }

    public void setListener(PlaybackControlListener l) {
        this.listener = l;
    }

    public void setLyricColor(int color) {
        this.currentColor = color;
        for (LyricItemDelegate delegate : lineDelegates) {
            delegate.setLyricColor(color);
        }
        invalidate();
    }

    public void setFontConfig(String config) {
        this.currentFontConfig = config;
        for (LyricItemDelegate delegate : lineDelegates) {
            delegate.setFontConfig(config);
        }
        requestLayout();
        invalidate();
    }

    public void setTextSize(float textSizeDp) {
        this.currentTextSizeDp = textSizeDp;
        for (LyricItemDelegate delegate : lineDelegates) {
            delegate.setTextSize(textSizeDp);
        }
        requestLayout();
        invalidate();
    }

    public void setUseSystemFont(boolean useSystemFont) {
        this.useSystemFont = useSystemFont;
        for (LyricItemDelegate delegate : lineDelegates) {
            delegate.setUseSystemFont(useSystemFont);
        }
        requestLayout();
        invalidate();
    }

    public void setEnableSparkles(boolean enableSparkles) {
        this.enableSparkles = enableSparkles;
        for (LyricItemDelegate delegate : lineDelegates) {
            delegate.setEnableSparkles(enableSparkles);
        }
        requestLayout();
        invalidate();
    }

    public void setLyrics(List<LyricLine> newLyrics, int hash) {
        if (lastLyricsHash == hash) return;
        lastLyricsHash = hash;
        cancelStaticScrollAnimator();
        if (newLyrics == null || newLyrics.isEmpty()) {
            lineDelegates.clear();
            targetIndex = -1;
            activeBlurIndex = -1;
            preciseScrollY = -1f;
            invalidate();
            return;
        }
        isInteractive = false;
        animate().cancel();
        animate().alpha(0f).setDuration(200L).withEndAction(() -> performLyricsSwap(newLyrics)).start();
    }

    private void performLyricsSwap(List<LyricLine> newLyrics) {
        cancelStaticScrollAnimator();
        lineDelegates.clear();
        targetIndex = -1;
        activeBlurIndex = -1;
        preciseScrollY = -1f;
        invalidate();
        final int finalAvailableWidth = getWidth() != 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        final int parentPaddingLeft = getPaddingLeft();
        final int parentPaddingRight = getPaddingRight();
        bgExecutor.execute(() -> {
            List<LyricItem> tempItems = new ArrayList<>();
            for (int i = 0; i < newLyrics.size(); i++) {
                LyricLine line = newLyrics.get(i);
                if (line.isLinkedBg || line.isRomaji) continue;
                if (line.isWaitingDots) {
                    long nextTime = -1;
                    for (int j = i + 1; j < newLyrics.size(); j++) {
                        LyricLine checkLine = newLyrics.get(j);
                        if (!checkLine.isWaitingDots && !checkLine.isBackground) {
                            nextTime = checkLine.time;
                            break;
                        }
                    }
                    if (nextTime != -1 && (nextTime - line.time) < 5000) continue;
                    if (nextTime != -1) line.endTime = (int) nextTime;
                }
                LyricItem item = new LyricItem(line, i);
                for (int j = i + 1; j < newLyrics.size(); j++) {
                    LyricLine next = newLyrics.get(j);
                    if (next.isWaitingDots || (!next.isLinkedBg && !next.isRomaji)) break;
                    if (next.isLinkedBg && item.linkedBgLine == null) {
                        item.linkedBgLine = next;
                        item.linkedBgIndex = j;
                    }
                    if (next.isRomaji && item.romajiLine == null) {
                        item.romajiLine = next;
                        item.romajiIndex = j;
                    }
                }
                tempItems.add(item);
            }
            List<LyricItemDelegate> tempDelegates = new ArrayList<>();
            int contentWidth = finalAvailableWidth - parentPaddingLeft - parentPaddingRight;
            for (LyricItem item : tempItems) {
                LyricItemDelegate delegate = new LyricItemDelegate(getContext(), item);
                if (currentColor != 0) delegate.setLyricColor(currentColor);
                if (currentFontConfig != null) delegate.setFontConfig(currentFontConfig);
                if (currentTextSizeDp > 0) delegate.setTextSize(currentTextSizeDp);
                if (useSystemFont) delegate.setUseSystemFont(useSystemFont);
                delegate.setEnableSparkles(enableSparkles);
                int textWidth = delegate.computeTextWidth(contentWidth);
                delegate.precomputeLayouts(textWidth);
                tempDelegates.add(delegate);
            }
            mainHandler.post(() -> {
                lineDelegates.clear();
                lineDelegates.addAll(tempDelegates);
                scrollTo(0, 0);
                preciseScrollY = 0f;
                requestLayout();
                invalidate();
                animate().alpha(1f).setDuration(250L).withEndAction(() -> isInteractive = true).start();
            });
        });
    }

    private long getEndTimeForLine(int rawIndex) {
        for (LyricItemDelegate delegate : lineDelegates) {
            if (delegate.getItem().mainIndex == rawIndex) {
                LyricLine line = delegate.getItem().mainLine;
                return line.endTime > 0 ? line.endTime : line.time + 5000;
            }
            if (delegate.getItem().romajiIndex == rawIndex) {
                LyricLine line = delegate.getItem().romajiLine;
                return line.endTime > 0 ? line.endTime : line.time + 5000;
            }
            if (delegate.getItem().linkedBgIndex == rawIndex) {
                LyricLine line = delegate.getItem().linkedBgLine;
                return line.endTime > 0 ? line.endTime : line.time + 5000;
            }
        }
        return 0;
    }

    private int findFirstVisibleIndex(float currentScrollY, int screenHeight) {
        if (lineDelegates.isEmpty()) return 0;
        for (int i = 0; i < lineDelegates.size(); i++) {
            LyricItemDelegate delegate = lineDelegates.get(i);
            if (delegate.getBottom() + delegate.staggerY >= currentScrollY) {
                return i;
            }
        }
        return 0;
    }

    private int findLastVisibleIndex(float currentScrollY, int screenHeight) {
        if (lineDelegates.isEmpty()) return 0;
        float viewportBottom = currentScrollY + screenHeight;
        for (int i = lineDelegates.size() - 1; i >= 0; i--) {
            LyricItemDelegate delegate = lineDelegates.get(i);
            if (delegate.getTop() + delegate.staggerY <= viewportBottom) {
                return i;
            }
        }
        return lineDelegates.size() - 1;
    }

    private void cancelStaticScrollAnimator() {
        if (staticScrollAnimator != null) {
            staticScrollAnimator.cancel();
            staticScrollAnimator = null;
        }
        staticScrollSourceY = -1f;
        staticScrollBaseTargetY = -1f;
        staticScrollTargetY = -1f;
        staticScrollShiftStartFraction = 0f;
        staticScrollShiftStartOffset = 0f;
        staticScrollShiftTargetOffset = 0f;
    }

    private boolean isStaticScrollRunning() {
        return staticScrollAnimator != null && staticScrollAnimator.isRunning();
    }

    @Override
    public void abortAnimatedScroll() {
        super.abortAnimatedScroll();
        cancelStaticScrollAnimator();
    }

    private float getTargetCenterY(int targetIdx, int screenHeight, float density) {
        if (targetIdx < 0 || targetIdx >= lineDelegates.size()) return screenHeight * 0.28f;
        float currentY = screenHeight * 0.28f;
        for (int i = 0; i < targetIdx; i++) {
            LyricItemDelegate delegate = lineDelegates.get(i);
            float h = delegate.getInactiveRestingHeight(density);
            float gap = 24 * density;
            if (i < lineDelegates.size() - 1) {
                LyricItemDelegate next = lineDelegates.get(i + 1);
                if (delegate.getItem().mainLine.isWaitingDots || next.getItem().mainLine.isWaitingDots) {
                    gap = 16 * density;
                } else if (delegate.getItem().mainLine.vocalType != next.getItem().mainLine.vocalType) {
                    gap = 40 * density;
                }
            }
            currentY += h + gap;
        }
        LyricItemDelegate target = lineDelegates.get(targetIdx);
        return currentY + target.getTargetRelativeCenterY(density);
    }

    private int findClosestLineIndexToScroll(float scrollY, int screenHeight, float density) {
        if (lineDelegates.isEmpty()) return 0;
        float focalY = screenHeight * 0.28f;
        float currentFocalY = scrollY + focalY;
        int closestIdx = 0;
        float minDiff = Float.MAX_VALUE;

        float currentY = screenHeight * 0.28f;
        for (int i = 0; i < lineDelegates.size(); i++) {
            LyricItemDelegate delegate = lineDelegates.get(i);
            float centerY = currentY + delegate.getTargetRelativeCenterY(density);
            float diff = Math.abs(currentFocalY - centerY);
            if (diff < minDiff) {
                minDiff = diff;
                closestIdx = i;
            }
            float h = delegate.getInactiveRestingHeight(density);
            float gap = 24 * density;
            if (i < lineDelegates.size() - 1) {
                LyricItemDelegate next = lineDelegates.get(i + 1);
                if (delegate.getItem().mainLine.isWaitingDots || next.getItem().mainLine.isWaitingDots) {
                    gap = 16 * density;
                } else if (delegate.getItem().mainLine.vocalType != next.getItem().mainLine.vocalType) {
                    gap = 40 * density;
                }
            }
            currentY += h + gap;
        }
        return closestIdx;
    }

    // Gold-standard fluid ease curve (Apple / CSS standard ease: cubic-bezier(0.25, 0.1, 0.25, 1.0))
    // Soft organic pickup so text doesn't whip away, carrying smooth momentum into a prolonged, velvety landing
    private static final PathInterpolator PREMIUM_STATIC_SCROLL_CURVE = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);

    private void performSmoothStaticScroll(float desiredScrollY) {
        cancelStaticScrollAnimator();
        abortAnimatedScroll();

        float currentScrollY = (preciseScrollY >= 0f) ? preciseScrollY : getScrollY();
        float distance = Math.abs(desiredScrollY - currentScrollY);
        float density = getResources().getDisplayMetrics().density;

        if (distance < 1f) {
            preciseScrollY = desiredScrollY;
            isPhysicsScrolling = true;
            scrollTo(getScrollX(), (int) desiredScrollY);
            isPhysicsScrolling = false;
            for (LyricItemDelegate delegate : lineDelegates) {
                delegate.staggerY = 0f;
                delegate.staggerVy = 0f;
                delegate.staggerDelay = 0f;
            }
            invalidate();
            return;
        }

        for (LyricItemDelegate delegate : lineDelegates) {
            delegate.staggerY = 0f;
            delegate.staggerVy = 0f;
            delegate.staggerDelay = 0f;
        }

        // Calm, luxurious fluid scroll: 800ms baseline for single-line transitions, smoothly scaling up to 1250ms for larger seeks
        int duration = (int) Math.min(1250, Math.max(800, 720 + (distance / (3.5f * density))));

        staticScrollSourceY = currentScrollY;
        staticScrollBaseTargetY = desiredScrollY;
        staticScrollTargetY = desiredScrollY;
        staticScrollShiftStartFraction = 0f;
        staticScrollShiftStartOffset = 0f;
        staticScrollShiftTargetOffset = 0f;

        staticScrollAnimator = ValueAnimator.ofFloat(0f, 1f);
        staticScrollAnimator.setDuration(duration);
        staticScrollAnimator.setInterpolator(PREMIUM_STATIC_SCROLL_CURVE);
        staticScrollAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            float baseInterpolated = (float) animation.getAnimatedValue();
            float val = staticScrollSourceY + (staticScrollBaseTargetY - staticScrollSourceY) * baseInterpolated;

            if (staticScrollShiftTargetOffset != 0f || staticScrollShiftStartOffset != 0f) {
                if (fraction >= staticScrollShiftStartFraction) {
                    float denom = Math.max(0.001f, 1f - staticScrollShiftStartFraction);
                    float s = Math.min(1f, Math.max(0f, (fraction - staticScrollShiftStartFraction) / denom));
                    float blend = s * s * (3f - 2f * s);
                    val += staticScrollShiftStartOffset + (staticScrollShiftTargetOffset - staticScrollShiftStartOffset) * blend;
                } else {
                    val += staticScrollShiftStartOffset;
                }
            }

            preciseScrollY = val;
            isPhysicsScrolling = true;
            scrollTo(getScrollX(), (int) val);
            isPhysicsScrolling = false;
        });
        staticScrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                staticScrollAnimator = null;
                staticScrollSourceY = -1f;
                staticScrollBaseTargetY = -1f;
                staticScrollTargetY = -1f;
                staticScrollShiftStartFraction = 0f;
                staticScrollShiftStartOffset = 0f;
                staticScrollShiftTargetOffset = 0f;
            }
        });
        staticScrollAnimator.start();
    }

    private void shiftStaticScrollTarget(int newTargetIndex) {
        if (newTargetIndex < 0 || newTargetIndex >= lineDelegates.size()) return;
        float density = getResources().getDisplayMetrics().density;
        int screenHeight = getHeight();
        if (screenHeight <= 0) {
            targetIndex = newTargetIndex;
            activeBlurIndex = newTargetIndex;
            return;
        }

        float activeMainCenterY = getTargetCenterY(newTargetIndex, screenHeight, density);
        float focalY = screenHeight * 0.28f;
        float newDesiredScrollY = Math.max(0, Math.min(activeMainCenterY - focalY, getScrollRange()));

        targetIndex = newTargetIndex;
        activeBlurIndex = newTargetIndex;

        if (!isStaticScrollRunning()) {
            performSmoothStaticScroll(newDesiredScrollY);
            return;
        }

        float currentTarget = (staticScrollTargetY >= 0f) ? staticScrollTargetY : staticScrollBaseTargetY;
        if (Math.abs(newDesiredScrollY - currentTarget) < 1f) {
            staticScrollTargetY = newDesiredScrollY;
            return;
        }

        long playTime = staticScrollAnimator.getCurrentPlayTime();
        long duration = staticScrollAnimator.getDuration();
        long remainingMs = duration - playTime;

        if (remainingMs >= 350) {
            // Ample flight time remaining: seamlessly redirect the scroll destination without restarting
            float fraction = staticScrollAnimator.getAnimatedFraction();
            float currentOffset = 0f;
            if (staticScrollShiftTargetOffset != 0f || staticScrollShiftStartOffset != 0f) {
                float denom = Math.max(0.001f, 1f - staticScrollShiftStartFraction);
                float s = Math.min(1f, Math.max(0f, (fraction - staticScrollShiftStartFraction) / denom));
                float blend = s * s * (3f - 2f * s);
                currentOffset = staticScrollShiftStartOffset + (staticScrollShiftTargetOffset - staticScrollShiftStartOffset) * blend;
            }

            staticScrollShiftStartFraction = fraction;
            staticScrollShiftStartOffset = currentOffset;
            staticScrollShiftTargetOffset = newDesiredScrollY - staticScrollBaseTargetY;
            staticScrollTargetY = newDesiredScrollY;
        } else {
            // In the final resting tail: initiate a calm static scroll into the next line
            performSmoothStaticScroll(newDesiredScrollY);
        }
    }

    private void performStaggeredScroll(int targetIdx, boolean isTap) {
        if (targetIdx < 0 || targetIdx >= lineDelegates.size()) return;
        if (getWidth() <= 0 || getHeight() <= 0) {
            targetIndex = targetIdx;
            activeBlurIndex = targetIdx;
            return;
        }

        if (!isTap && isStaticScrollRunning()) {
            shiftStaticScrollTarget(targetIdx);
            return;
        }

        cancelStaticScrollAnimator();
        abortAnimatedScroll();

        float density = getResources().getDisplayMetrics().density;
        int screenHeight = getHeight();
        getCurrentY(screenHeight, density);

        float activeMainCenterY = getTargetCenterY(targetIdx, screenHeight, density);
        float focalY = screenHeight * 0.28f;
        float desiredScrollY = Math.max(0, Math.min(activeMainCenterY - focalY, getScrollRange()));

        float currentScrollY = (preciseScrollY >= 0f) ? preciseScrollY : getScrollY();
        float shift = desiredScrollY - currentScrollY;

        targetIndex = targetIdx;
        activeBlurIndex = targetIdx;

        if (useStaticScroll) {
            performSmoothStaticScroll(desiredScrollY);
            return;
        }

        if (Math.abs(shift) < 1f) {
            preciseScrollY = desiredScrollY;
            isPhysicsScrolling = true;
            scrollTo(getScrollX(), (int) desiredScrollY);
            isPhysicsScrolling = false;
            invalidate();
            return;
        }

        executeStaggeredTouchdown(desiredScrollY, shift, screenHeight, density);
    }

    private void executeStaggeredTouchdown(float desiredScrollY, float staggerShift, int screenHeight, float density) {
        float currentScrollY = (preciseScrollY >= 0f) ? preciseScrollY : getScrollY();

        int firstVis = findFirstVisibleIndex(currentScrollY, screenHeight);
        int lastVis = findLastVisibleIndex(currentScrollY, screenHeight);
        if (lastVis < firstVis) lastVis = firstVis;
        int visSpan = Math.max(1, lastVis - firstVis);

        float delayPerLine = 0.045f;

        // Cascade wave from top visible line DOWNWARDS (line 1 -> line 2 -> line 3...)
        // For forward scroll (staggerShift > 0): lines move UP one by one (top line moves first, pulling away from line under it).
        // For backward scroll (staggerShift < 0): lines move DOWN one by one (top line moves down first, getting closer to line under it).
        for (int i = 0; i < lineDelegates.size(); i++) {
            LyricItemDelegate delegate = lineDelegates.get(i);
            int step;
            if (i < firstVis) {
                step = 0;
            } else {
                step = Math.min(visSpan + 2, i - firstVis);
            }
            float delay = step * delayPerLine;
            delegate.staggerY += staggerShift;
            delegate.staggerVy = 0f;
            delegate.staggerDelay = delay;
        }

        preciseScrollY = desiredScrollY;
        isPhysicsScrolling = true;
        scrollTo(getScrollX(), (int) desiredScrollY);
        isPhysicsScrolling = false;

        invalidate();
    }

    public void updateLyricsProgress(int progressMs, boolean justSeeked) {
        if (lineDelegates.isEmpty()) return;

        long now = SystemClock.uptimeMillis();
        if (lockedTargetIndex >= 0 && lockedTargetIndex < lineDelegates.size()) {
            long targetTime = (lockedSeekTimestamp != -1L)
                    ? lockedSeekTimestamp
                    : lineDelegates.get(lockedTargetIndex).getItem().mainLine.time;
            long diff = Math.abs(progressMs - targetTime);
            if (diff <= 800 || now >= manualSeekLockUntil) {
                lockedTargetIndex = -1;
                lockedSeekTimestamp = -1L;
            } else {
                // Discard stale playback updates from ExoPlayer while the seek lands (both forward and backward)!
                return;
            }
        }

        if (!justSeeked && lastProgressMs != -1 && Math.abs(progressMs - lastProgressMs) > 1500) {
            justSeeked = true;
        }
        lastProgressMs = progressMs;

        int newActiveIndex = -1;
        int nextUpcomingIndex = -1;

        for (int i = 0; i < lineDelegates.size(); i++) {
            LyricItemDelegate delegate = lineDelegates.get(i);
            LyricItem item = delegate.getItem();
            long mainEndTime = getEndTimeForLine(item.mainIndex);

            if (item.linkedBgLine != null) {
                mainEndTime = Math.max(mainEndTime, getEndTimeForLine(item.linkedBgIndex));
            }

            boolean isActive;
            if (item.mainLine.isWaitingDots) {
                isActive = progressMs >= item.mainLine.time && progressMs <= item.mainLine.endTime;
                if (justSeeked && progressMs > item.mainLine.time + 500) isActive = false;
            } else if (item.mainLine.isBackground) {
                isActive = progressMs >= (item.mainLine.time - 1000) && progressMs <= mainEndTime;
            } else {
                isActive = progressMs >= item.mainLine.time && progressMs <= mainEndTime;
            }

            delegate.isActive = isActive;
            delegate.syncInnerProgress(progressMs, justSeeked, mainEndTime);

            if (justSeeked) {
                delegate.snapState(isActive);
            }

            if (isActive && newActiveIndex == -1) {
                newActiveIndex = i;
            }

            if (!isActive && progressMs < item.mainLine.time && nextUpcomingIndex == -1) {
                nextUpcomingIndex = i;
            }
        }

        if (newActiveIndex == -1 && lyricAnticipation && nextUpcomingIndex != -1) {
            newActiveIndex = nextUpcomingIndex;
        }

        if (newActiveIndex != -1 && newActiveIndex != targetIndex) {
            if (isStaticScrollRunning()) {
                shiftStaticScrollTarget(newActiveIndex);
            } else if (!isAutoScrollPaused) {
                performStaggeredScroll(newActiveIndex, false);
            } else {
                targetIndex = newActiveIndex;
                activeBlurIndex = newActiveIndex;
            }
        }
        invalidate();
    }

    @Override
    protected void onMeasureForChild(int widthMeasureSpec, int heightMeasureSpec) {
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        for (LyricItemDelegate delegate : lineDelegates) {
            delegate.measureStaticBounds(availableWidth, getPaddingLeft(), getPaddingRight());
        }
        float density = getResources().getDisplayMetrics().density;
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int screenHeight = hSize != 0 ? hSize : (getMeasuredHeight() != 0 ? getMeasuredHeight() : getResources().getDisplayMetrics().heightPixels);
        float currentY = getCurrentY(screenHeight, density);
        setChildMeasuredDimension(availableWidth, (int) (currentY + (screenHeight * 0.75f)));
    }

    private float getCurrentY(int screenHeight, float density) {
        float currentY = screenHeight * 0.28f;
        for (int i = 0; i < lineDelegates.size(); i++) {
            LyricItemDelegate delegate = lineDelegates.get(i);
            delegate.updateDynamicHeights(density);
            delegate.setTop(currentY);
            float gap = 24 * density;
            if (i < lineDelegates.size() - 1) {
                LyricItemDelegate next = lineDelegates.get(i + 1);
                if (delegate.getItem().mainLine.isWaitingDots || next.getItem().mainLine.isWaitingDots) {
                    gap = 16 * density;
                } else if (delegate.getItem().mainLine.vocalType != next.getItem().mainLine.vocalType) {
                    gap = 40 * density;
                }
            }
            currentY += delegate.getLiveHeight() + gap;
        }
        liveContentBottom = currentY;
        return currentY;
    }

    private float liveContentBottom = 0f;

    @Override
    public int getScrollRange() {
        int screenHeight = getHeight();
        if (screenHeight > 0 && liveContentBottom > 0f) {
            int childSize = (int) (liveContentBottom + (screenHeight * 0.75f));
            int parentSpace = screenHeight - getPaddingTop() - getPaddingBottom();
            return Math.max(0, childSize - parentSpace);
        }
        return super.getScrollRange();
    }

    @Override
    protected void onLayoutForChild(int l, int t, int r, int b) {}

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (!isPhysicsScrolling) {
            cancelStaticScrollAnimator();
            preciseScrollY = t;
            isAutoScrollPaused = true;
            removeCallbacks(recoveryRunnable);
            postDelayed(recoveryRunnable, 2500);

            // Finger dragging: keep lines strictly glued to scroll position with zero residual stagger
            for (LyricItemDelegate delegate : lineDelegates) {
                delegate.staggerY = 0f;
                delegate.staggerVy = 0f;
                delegate.staggerDelay = 0f;
            }
        }
    }

    @Override
    protected void onDrawForChild(@NonNull Canvas canvas) {
        if (lineDelegates.isEmpty()) return;
        long now = SystemClock.uptimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (dt > 0.033f) dt = 0.033f;
        for (LyricItemDelegate delegate : lineDelegates) delegate.updatePhysics(dt);
        float density = getResources().getDisplayMetrics().density;
        int screenHeight = getHeight();
        getCurrentY(screenHeight, density);

        float subpixelOffset = preciseScrollY - getScrollY();
        int sc = canvas.save();
        canvas.translate(0, -subpixelOffset);

        float cullingBuffer = 200f * density;
        int scrollY = getScrollY();
        int viewportBottom = scrollY + screenHeight;
        for (int i = 0; i < lineDelegates.size(); i++) {
            LyricItemDelegate delegate = lineDelegates.get(i);
            float visualTop = delegate.getTop() + delegate.staggerY;
            float visualBottom = delegate.getBottom() + delegate.staggerY;
            if (visualBottom < scrollY - cullingBuffer || visualTop > viewportBottom + cullingBuffer) continue;

            if (enableBlurs && !isAutoScrollPaused && activeBlurIndex >= 0) {
                int distance = Math.abs(i - activeBlurIndex);
                if (distance == 0 || delegate.isStillActiveOrFading()) {
                    delegate.setTargetBlurRadius(0f);
                } else {
                    float targetR = Math.min(distance * 3.5f, 16f);
                    delegate.setTargetBlurRadius(targetR);
                }
            } else {
                delegate.setTargetBlurRadius(0f);
            }

            delegate.draw(canvas, getPaddingLeft(), density, getWidth());
        }
        canvas.restoreToCount(sc);
        postInvalidateOnAnimation();
    }

    public void selectLineExplicitly(int index, int seekTime) {
        if (index < 0 || index >= lineDelegates.size()) return;
        abortAnimatedScroll();

        lastProgressMs = seekTime;
        lockedTargetIndex = index;
        lockedSeekTimestamp = seekTime;
        manualSeekLockUntil = SystemClock.uptimeMillis() + 1000L;

        for (int i = 0; i < lineDelegates.size(); i++) {
            LyricItemDelegate delegate = lineDelegates.get(i);
            LyricItem item = delegate.getItem();
            long mainEndTime = getEndTimeForLine(item.mainIndex);
            if (item.linkedBgLine != null) {
                mainEndTime = Math.max(mainEndTime, getEndTimeForLine(item.linkedBgIndex));
            }
            boolean isActive = (i == index);
            delegate.isActive = isActive;
            delegate.syncInnerProgress(seekTime, true, mainEndTime);
        }

        targetIndex = index;
        activeBlurIndex = index;

        if (!isAutoScrollPaused) {
            // Auto scroll is currently active: scroll view to selected line immediately
            performStaggeredScroll(index, true);
        } else {
            // Auto scroll is disabled: do not scroll view and do not re-enable auto scroll;
            // only the runnable cooldown brings auto scroll back!
            removeCallbacks(recoveryRunnable);
            postDelayed(recoveryRunnable, 2500);
            invalidate();
        }
    }

    @Override
    protected boolean onTouchEventForChild(@NonNull MotionEvent event) {
        if (!isInteractive) return false;
        int action = event.getActionMasked();
        float y = event.getY();
        float density = getResources().getDisplayMetrics().density;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                abortAnimatedScroll();
                touchStartY = y;
                isActuallyScrolling = false;
                pressedDelegate = null;
                pressedPart = 0;
                float tapY = y;
                for (LyricItemDelegate delegate : lineDelegates) {
                    if (tapY >= delegate.getTop() + delegate.staggerY && tapY <= delegate.getBottom() + delegate.staggerY) {
                        int part = delegate.getPartAt(tapY, density);
                        if (part != 0) {
                            pressedDelegate = delegate;
                            pressedPart = part;
                            delegate.setPressed(part, true);
                        }
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isActuallyScrolling && Math.abs(y - touchStartY) > touchSlop) {
                    isActuallyScrolling = true;
                    isPhysicsScrolling = false;
                    isAutoScrollPaused = true;
                    removeCallbacks(recoveryRunnable);
                    abortAnimatedScroll();
                    if (pressedDelegate != null) {
                        pressedDelegate.setPressed(0, false);
                        pressedDelegate = null;
                        pressedPart = 0;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                abortAnimatedScroll();
                if (!isActuallyScrolling && pressedDelegate != null) {
                    float releaseY = y;
                    int part = pressedDelegate.getPartAt(releaseY, density);
                    if (part == pressedPart) {
                        int tappedIndex = lineDelegates.indexOf(pressedDelegate);
                        long seekTime = -1;
                        if (part == 1) {
                            seekTime = pressedDelegate.getItem().mainLine.time;
                        } else if (part == 2 && pressedDelegate.getItem().linkedBgLine != null) {
                            seekTime = pressedDelegate.getItem().linkedBgLine.time;
                        }
                        if (seekTime != -1 && tappedIndex != -1) {
                            selectLineExplicitly(tappedIndex, (int) seekTime);
                            if (listener != null) {
                                listener.onSeekRequested(seekTime);
                            }
                        }
                    }
                }
                if (pressedDelegate != null) {
                    pressedDelegate.setPressed(0, false);
                }
                pressedDelegate = null;
                pressedPart = 0;
                isActuallyScrolling = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                abortAnimatedScroll();
                if (pressedDelegate != null) {
                    pressedDelegate.setPressed(0, false);
                }
                pressedDelegate = null;
                pressedPart = 0;
                isActuallyScrolling = false;
                break;
        }
        return true;
    }
}

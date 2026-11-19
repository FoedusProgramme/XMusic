package com.xapps.media.xmusic.ui.lyrics;
import com.xapps.media.xmusic.ui.scroll.*;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XLyricsView extends NestedScrollView {

    private final List<XLyricsItemDelegate> lineDelegates = new ArrayList<>();
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
    private boolean enableSparkles = false;
    private boolean lyricAnticipation = false;
    private boolean enableBlurs = false;
    private boolean isDebugMode = false;

    private long debugLastNanoTime = 0;
    private float debugFps = 0f;
    private float debugFrameTimeMs = 0f;
    private int debugFrameCount = 0;
    private long debugFpsLastCalcTime = 0;
    private Paint debugBgPaint;
    private Paint debugTextPaint;
    private Paint debugHeaderPaint;

    private float touchStartY = 0f;
    private int touchSlop;
    private XLyricsItemDelegate pressedDelegate;
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

    public XLyricsView(@NonNull Context context) {
        this(context, null);
    }

    public XLyricsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipToOutline(false);
    }

    public void setDebugMode(boolean debugMode) {
        this.isDebugMode = debugMode;
        invalidate();
    }

    public boolean isDebugMode() {
        return isDebugMode;
    }

    public void setEnableBlurs(boolean enable) {
        this.enableBlurs = enable;
        if (!enable) {
            for (XLyricsItemDelegate delegate : lineDelegates) {
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
            for (XLyricsItemDelegate delegate : lineDelegates) {
                delegate.staggerY = 0f;
                delegate.staggerVy = 0f;
                delegate.staggerDelay = 0f;
            }
            invalidate();
        }
    }

    public void setListener(PlaybackControlListener l) {
        this.listener = l;
    }

    public void setLyricColor(int color) {
        this.currentColor = color;
        for (XLyricsItemDelegate delegate : lineDelegates) {
            delegate.setLyricColor(color);
        }
        invalidate();
    }

    public void setFontConfig(String config) {
        this.currentFontConfig = config;
        for (XLyricsItemDelegate delegate : lineDelegates) {
            delegate.setFontConfig(config);
        }
        requestLayout();
        invalidate();
    }

    public void setTextSize(float textSizeDp) {
        this.currentTextSizeDp = textSizeDp;
        for (XLyricsItemDelegate delegate : lineDelegates) {
            delegate.setTextSize(textSizeDp);
        }
        requestLayout();
        invalidate();
    }

    public void setUseSystemFont(boolean useSystemFont) {
        this.useSystemFont = useSystemFont;
        for (XLyricsItemDelegate delegate : lineDelegates) {
            delegate.setUseSystemFont(useSystemFont);
        }
        requestLayout();
        invalidate();
    }

    public void setEnableSparkles(boolean enableSparkles) {
        this.enableSparkles = enableSparkles;
        for (XLyricsItemDelegate delegate : lineDelegates) {
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

    public void recoverScroll() {
        mainHandler.post(recoveryRunnable);
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
            List<XLyricsItemDelegate> tempDelegates = new ArrayList<>();
            int contentWidth = finalAvailableWidth - parentPaddingLeft - parentPaddingRight;
            for (LyricItem item : tempItems) {
                XLyricsItemDelegate delegate = new XLyricsItemDelegate(getContext(), item);
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
        for (XLyricsItemDelegate delegate : lineDelegates) {
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
            XLyricsItemDelegate delegate = lineDelegates.get(i);
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
            XLyricsItemDelegate delegate = lineDelegates.get(i);
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
            XLyricsItemDelegate delegate = lineDelegates.get(i);
            float h = delegate.getInactiveRestingHeight(density);
            float gap = 24 * density;
            if (i < lineDelegates.size() - 1) {
                XLyricsItemDelegate next = lineDelegates.get(i + 1);
                if (delegate.getItem().mainLine.isWaitingDots || next.getItem().mainLine.isWaitingDots) {
                    gap = 16 * density;
                } else if (delegate.getItem().mainLine.vocalType != next.getItem().mainLine.vocalType) {
                    gap = 40 * density;
                }
            }
            currentY += h + gap;
        }
        XLyricsItemDelegate target = lineDelegates.get(targetIdx);
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
            XLyricsItemDelegate delegate = lineDelegates.get(i);
            float centerY = currentY + delegate.getTargetRelativeCenterY(density);
            float diff = Math.abs(currentFocalY - centerY);
            if (diff < minDiff) {
                minDiff = diff;
                closestIdx = i;
            }
            float h = delegate.getInactiveRestingHeight(density);
            float gap = 24 * density;
            if (i < lineDelegates.size() - 1) {
                XLyricsItemDelegate next = lineDelegates.get(i + 1);
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
            for (XLyricsItemDelegate delegate : lineDelegates) {
                delegate.staggerY = 0f;
                delegate.staggerVy = 0f;
                delegate.staggerDelay = 0f;
            }
            invalidate();
            return;
        }

        for (XLyricsItemDelegate delegate : lineDelegates) {
            delegate.staggerY = 0f;
            delegate.staggerVy = 0f;
            delegate.staggerDelay = 0f;
        }

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

        for (int i = 0; i < lineDelegates.size(); i++) {
            XLyricsItemDelegate delegate = lineDelegates.get(i);
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
            XLyricsItemDelegate delegate = lineDelegates.get(i);
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
        for (XLyricsItemDelegate delegate : lineDelegates) {
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
            XLyricsItemDelegate delegate = lineDelegates.get(i);
            delegate.updateDynamicHeights(density);
            delegate.setTop(currentY);
            float gap = 24 * density;
            if (i < lineDelegates.size() - 1) {
                XLyricsItemDelegate next = lineDelegates.get(i + 1);
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

            for (XLyricsItemDelegate delegate : lineDelegates) {
                delegate.staggerY = 0f;
                delegate.staggerVy = 0f;
                delegate.staggerDelay = 0f;
            }
        }
    }

    @Override
    protected void onDrawForChild(@NonNull Canvas canvas) {
        if (lineDelegates.isEmpty()) {
            if (isDebugMode) {
                drawDebugOverlay(canvas, 0);
                postInvalidateOnAnimation();
            }
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (dt > 0.033f) dt = 0.033f;
        for (XLyricsItemDelegate delegate : lineDelegates) delegate.updatePhysics(dt);
        float density = getResources().getDisplayMetrics().density;
        int screenHeight = getHeight();
        getCurrentY(screenHeight, density);

        float subpixelOffset = preciseScrollY - getScrollY();
        int sc = canvas.save();
        canvas.translate(0, -subpixelOffset);

        float cullingBuffer = 200f * density;
        int scrollY = getScrollY();
        int viewportBottom = scrollY + screenHeight;
        int visibleCount = 0;
        for (int i = 0; i < lineDelegates.size(); i++) {
            XLyricsItemDelegate delegate = lineDelegates.get(i);
            float visualTop = delegate.getTop() + delegate.staggerY;
            float visualBottom = delegate.getBottom() + delegate.staggerY;
            if (visualBottom < scrollY - cullingBuffer || visualTop > viewportBottom + cullingBuffer) continue;

            visibleCount++;

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

        if (isDebugMode) {
            drawDebugOverlay(canvas, visibleCount);
        }

        postInvalidateOnAnimation();
    }

    private void initDebugPaints() {
        if (debugBgPaint == null) {
            float density = getResources().getDisplayMetrics().density;

            debugBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            debugBgPaint.setColor(0xDD121212);

            debugTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            debugTextPaint.setColor(0xFFFFFFFF);
            debugTextPaint.setTextSize(11f * density);
            debugTextPaint.setTypeface(Typeface.MONOSPACE);

            debugHeaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            debugHeaderPaint.setColor(0xFF00FF88);
            debugHeaderPaint.setTextSize(12f * density);
            debugHeaderPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        }
    }

    private void drawDebugOverlay(@NonNull Canvas canvas, int visibleCount) {
        initDebugPaints();
        float density = getResources().getDisplayMetrics().density;

        long nowNanos = System.nanoTime();
        if (debugLastNanoTime > 0) {
            float frameMs = (nowNanos - debugLastNanoTime) / 1_000_000f;
            debugFrameTimeMs = debugFrameTimeMs * 0.85f + frameMs * 0.15f;
        }
        debugLastNanoTime = nowNanos;

        debugFrameCount++;
        long nowMs = SystemClock.uptimeMillis();
        if (nowMs - debugFpsLastCalcTime >= 400) {
            debugFps = (debugFrameCount * 1000f) / Math.max(1, (nowMs - debugFpsLastCalcTime));
            debugFrameCount = 0;
            debugFpsLastCalcTime = nowMs;
        }

        List<String> lines = new ArrayList<>();
        lines.add("PERFORMANCE & LYRICS DEBUG");

        int fpsColor;
        if (debugFps >= 55f) fpsColor = 0xFF00FF88;
        else if (debugFps >= 35f) fpsColor = 0xFFFFCC00;
        else fpsColor = 0xFFFF4444;

        lines.add(String.format(Locale.US, "FPS: %.1f | Frame: %.2f ms", debugFps, debugFrameTimeMs));
        lines.add(String.format(Locale.US, "Lines: %d total / %d visible", lineDelegates.size(), visibleCount));
        lines.add(String.format(Locale.US, "Target Line: %d | Blur Index: %d", targetIndex, activeBlurIndex));
        lines.add(String.format(Locale.US, "ScrollY: %d | Precise: %.1f", getScrollY(), preciseScrollY));
        lines.add(String.format(Locale.US, "Flags: [Static:%b] [Paused:%b]", useStaticScroll, isAutoScrollPaused));
        lines.add(String.format(Locale.US, "Anim: [StaticAnim:%b] [Physics:%b]", isStaticScrollRunning(), isPhysicsScrolling));

        float padding = 10f * density;
        float lineHeight = 15f * density;
        float width = 250f * density;
        float height = padding * 2 + lines.size() * lineHeight;

        float startX = getScrollX() + 16f * density;
        float startY = getScrollY() + 16f * density;

        RectF bgRect = new RectF(startX, startY, startX + width, startY + height);
        canvas.drawRoundRect(bgRect, 10f * density, 10f * density, debugBgPaint);

        float textX = startX + padding;
        float textY = startY + padding + 11f * density;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0) {
                canvas.drawText(line, textX, textY, debugHeaderPaint);
            } else if (i == 1) {
                debugTextPaint.setColor(fpsColor);
                canvas.drawText(line, textX, textY, debugTextPaint);
                debugTextPaint.setColor(0xFFFFFFFF);
            } else {
                canvas.drawText(line, textX, textY, debugTextPaint);
            }
            textY += lineHeight;
        }
    }

    public void selectLineExplicitly(int index, int seekTime) {
        if (index < 0 || index >= lineDelegates.size()) return;
        abortAnimatedScroll();

        lastProgressMs = seekTime;
        lockedTargetIndex = index;
        lockedSeekTimestamp = seekTime;
        manualSeekLockUntil = SystemClock.uptimeMillis() + 1000L;

        for (int i = 0; i < lineDelegates.size(); i++) {
            XLyricsItemDelegate delegate = lineDelegates.get(i);
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
            performStaggeredScroll(index, true);
        } else {
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
                for (XLyricsItemDelegate delegate : lineDelegates) {
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

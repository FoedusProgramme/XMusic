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
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.view.View;
import com.xapps.media.xmusic.models.LyricItem;
import com.xapps.media.xmusic.models.LyricLine;

public class LyricItemDelegate {

    private final LyricItem item;
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private XLyricsLineView mainLineView;
    private XLyricsLineView bgLineView;
    private XLyricsRomajiLineView romajiView;
    private XWaitingDotsView dotsView;

    private float topY = 0;
    private float calculatedHeight = 0;
    private int paddingLeft = 0;
    private int paddingRight = 0;

    private int rawDotsHeight = 0;
    public int rawMainHeight = 0;
    private int rawBgHeight = 0;
    private int rawRomajiHeight = 0;

    private int preppedWidth = -1;
    private boolean isPrepped = false;
    public boolean isActive = false;
    private float activeProgress = 0f;
    private float activeProgressVelocity = 0f;

    public float staggerY = 0f;
    public float staggerVy = 0f;
    public float staggerDelay = 0f;
    public float stiffness = 145f;
    public float dampingRatio = 0.94f;

    private int currentColor = 0xFFFFFFFF;
    private boolean mainPressed = false;
    private boolean bgPressed = false;
    private float mainRippleAlpha = 0f;
    private float bgRippleAlpha = 0f;

    private static final BlurMaskFilter[] BLUR_CACHE = new BlurMaskFilter[64];
    static {
        for (int i = 0; i < 64; i++) {
            BLUR_CACHE[i] = new BlurMaskFilter((i + 1) * 0.25f, BlurMaskFilter.Blur.NORMAL);
        }
    }
    private float currentBlurRadius = 0f;
    private float targetBlurRadius = 0f;
    private BlurMaskFilter lastAppliedFilter = null;

    public LyricItemDelegate(Context context, LyricItem item) {
        this.item = item;
        initViews(context);
    }

    public static boolean isStartingRtl(CharSequence text) {
        if (text == null) return false;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            int cp = Character.codePointAt(text, i);
            if (Character.isSupplementaryCodePoint(cp)) {
                i++;
            }
            byte dir = Character.getDirectionality(cp);
            if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT
                    || dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING
                    || dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE) {
                return false;
            }
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                    || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                    || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsRtlChar(CharSequence text) {
        if (text == null) return false;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if ((c >= '\u0590' && c <= '\u08FF')
                    || (c >= '\uFB1D' && c <= '\uFDFF')
                    || (c >= '\uFE70' && c <= '\uFEFF')) {
                return true;
            }
            byte dir = Character.getDirectionality(c);
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                    || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                    || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE) {
                return true;
            }
        }
        return false;
    }

    public boolean isVisuallyRightAligned() {
        if (item.mainLine == null || item.mainLine.isWaitingDots) return false;
        boolean startsRtl = isStartingRtl(item.mainLine.line);
        boolean isSecondaryVocal = !item.mainLine.isBackground && item.mainLine.vocalType == 2;
        return startsRtl ^ isSecondaryVocal;
    }

    private Layout.Alignment resolveAlignment(CharSequence subText, boolean visuallyRight) {
        boolean textRtl = isStartingRtl(subText);
        return (visuallyRight != textRtl) ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;
    }

    public int computeTextWidth(int contentWidth) {
        boolean rightAligned = isVisuallyRightAligned();
        int pLeft = (int) (contentWidth * (rightAligned ? 0.2f : 0.05f));
        int pRight = (int) (contentWidth * (rightAligned ? 0.05f : 0.2f));
        return contentWidth - pLeft - pRight;
    }

    private void initViews(Context context) {
        if (item.mainLine == null) return;
        if (item.mainLine.isWaitingDots) {
            dotsView = new XWaitingDotsView(context);
        } else if (item.mainLine.isBackground) {
            bgLineView = createLineView(context, item.mainLine);
        } else {
            mainLineView = createLineView(context, item.mainLine);
            if (item.linkedBgLine != null) {
                bgLineView = createLineView(context, item.linkedBgLine);
                bgLineView.setTextSize(20);
            }
        }
        if (item.romajiLine != null) {
            romajiView = new XLyricsRomajiLineView(context);
            romajiView.setLyricLine(item.romajiLine);
        }
    }

    private XLyricsLineView createLineView(Context context, LyricLine line) {
        if (line.line != null && containsRtlChar(line.line)) {
            XLyricsBidiLineView v = new XLyricsBidiLineView(context);
            v.setLyricLine(line);
            return v;
        }
        XLyricsLineView v = new XLyricsLineView(context);
        v.setLyricLine(line);
        return v;
    }

    public void precomputeLayouts(int textWidth) {
        boolean visuallyRight = isVisuallyRightAligned();
        if (mainLineView != null && item.mainLine != null && item.mainLine.line != null) {
            mainLineView.setLineGravity(resolveAlignment(item.mainLine.line, visuallyRight));
            mainLineView.precomputeLayoutData(item.mainLine.line.toString(), textWidth);
        }
        if (bgLineView != null) {
            LyricLine bgSource = item.linkedBgLine != null ? item.linkedBgLine : item.mainLine;
            if (bgSource != null && bgSource.line != null) {
                bgLineView.setLineGravity(resolveAlignment(bgSource.line, visuallyRight));
                bgLineView.precomputeLayoutData(bgSource.line.toString(), textWidth);
            }
        }
        if (romajiView != null && item.romajiLine != null && item.romajiLine.line != null) {
            romajiView.setLineGravity(resolveAlignment(item.romajiLine.line, visuallyRight));
            romajiView.precomputeLayoutData(item.romajiLine.line.toString(), textWidth);
        }
    }

    public void measureStaticBounds(int availableWidth, int parentPaddingLeft, int parentPaddingRight) {
        int contentWidth = availableWidth - parentPaddingLeft - parentPaddingRight;
        boolean visuallyRight = isVisuallyRightAligned();
        paddingLeft = (int) (contentWidth * (visuallyRight ? 0.2f : 0.05f));
        paddingRight = (int) (contentWidth * (visuallyRight ? 0.05f : 0.2f));

        int textWidth = contentWidth - paddingLeft - paddingRight;
        if (textWidth != preppedWidth) {
            isPrepped = false;
            preppedWidth = textWidth;
        }
        if (dotsView != null) {
            assert item.mainLine != null;
            dotsView.setTimes(item.mainLine.time, item.mainLine.endTime);
            dotsView.measure(View.MeasureSpec.makeMeasureSpec(textWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            rawDotsHeight = dotsView.getMeasuredHeight();
            dotsView.layout(0, 0, textWidth, rawDotsHeight);
        }
        if (mainLineView != null) {
            assert item.mainLine != null;
            mainLineView.setLineGravity(resolveAlignment(item.mainLine.line, visuallyRight));
            rawMainHeight = mainLineView.getDesiredHeight(item.mainLine.line.toString(), textWidth);
        }
        if (bgLineView != null) {
            LyricLine bgSource = item.linkedBgLine != null ? item.linkedBgLine : item.mainLine;
            assert bgSource != null;
            bgLineView.setLineGravity(resolveAlignment(bgSource.line, visuallyRight));
            rawBgHeight = bgLineView.getDesiredHeight(bgSource.line.toString(), textWidth);
        }
        if (romajiView != null) {
            romajiView.setLineGravity(resolveAlignment(item.romajiLine.line, visuallyRight));
            rawRomajiHeight = romajiView.getDesiredHeight(item.romajiLine.line.toString(), textWidth);
        }
    }

    private void invalidateMetrics() {
        isPrepped = false;
        rawDotsHeight = 0;
        rawMainHeight = 0;
        rawBgHeight = 0;
        rawRomajiHeight = 0;
        preppedWidth = -1;
    }

    private void prep(int textWidth) {
        if (isPrepped) return;
        boolean visuallyRight = isVisuallyRightAligned();
        if (mainLineView != null) {
            mainLineView.setLineGravity(resolveAlignment(item.mainLine.line, visuallyRight));
            mainLineView.setText(item.mainLine.line.toString(), textWidth);
            mainLineView.measure(View.MeasureSpec.makeMeasureSpec(textWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(rawMainHeight, View.MeasureSpec.EXACTLY));
            mainLineView.layout(0, 0, textWidth, rawMainHeight);
        }
        if (bgLineView != null) {
            LyricLine bgSource = item.linkedBgLine != null ? item.linkedBgLine : item.mainLine;
            bgLineView.setLineGravity(resolveAlignment(bgSource.line, visuallyRight));
            bgLineView.setText(bgSource.line.toString(), textWidth);
            bgLineView.measure(View.MeasureSpec.makeMeasureSpec(textWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(rawBgHeight, View.MeasureSpec.EXACTLY));
            bgLineView.layout(0, 0, textWidth, rawBgHeight);
        }
        if (romajiView != null) {
            romajiView.setLineGravity(resolveAlignment(item.romajiLine.line, visuallyRight));
            romajiView.setText(item.romajiLine.line.toString(), textWidth);
            romajiView.measure(View.MeasureSpec.makeMeasureSpec(textWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(rawRomajiHeight, View.MeasureSpec.EXACTLY));
            romajiView.layout(0, 0, textWidth, rawRomajiHeight);
        }
        isPrepped = true;
    }

    public void snapState(boolean active) {
        this.isActive = active;
        if (active) {
            this.currentBlurRadius = 0f;
            this.targetBlurRadius = 0f;
            this.lastAppliedFilter = null;
            setBlurFilter(null);
        } else {
            if (bgLineView != null) {
                bgLineView.finishColorTransition();
            }
        }
    }

    public void syncInnerProgress(int progressMs, boolean snap, long parentEndTime) {
        if (dotsView != null) dotsView.updateProgress(progressMs);
        if (mainLineView != null) {
            mainLineView.setCustomEndTime(parentEndTime);
            mainLineView.updateProgress(progressMs, snap);
        }
        if (bgLineView != null) {
            bgLineView.setCustomEndTime(parentEndTime);
            bgLineView.updateProgress(progressMs, snap);
        }
        if (romajiView != null) romajiView.updateProgress(progressMs, snap);
    }

    public void updatePhysics(float dt) {
        float targetProgress = isActive ? 1f : 0f;
        float progDisplacement = activeProgress - targetProgress;
        float progAccel = (-140f * progDisplacement) - (22.5f * activeProgressVelocity);
        activeProgressVelocity += progAccel * dt;
        activeProgress += activeProgressVelocity * dt;
        if (!isActive && activeProgress < 0.0005f && Math.abs(activeProgressVelocity) < 0.005f) {
            activeProgress = 0f;
            activeProgressVelocity = 0f;
            if (bgLineView != null) {
                bgLineView.finishColorTransition();
            }
        } else if (isActive && activeProgress > 0.9995f && Math.abs(activeProgressVelocity) < 0.005f) {
            activeProgress = 1f;
            activeProgressVelocity = 0f;
        }

        float mainTargetAlpha = mainPressed ? 1f : 0f;
        mainRippleAlpha += (mainTargetAlpha - mainRippleAlpha) * (mainPressed ? 15f : 8f) * dt;
        if (Math.abs(mainTargetAlpha - mainRippleAlpha) < 0.001f) mainRippleAlpha = mainTargetAlpha;

        float bgTargetAlpha = bgPressed ? 1f : 0f;
        bgRippleAlpha += (bgTargetAlpha - bgRippleAlpha) * (bgPressed ? 15f : 8f) * dt;
        if (Math.abs(bgTargetAlpha - bgRippleAlpha) < 0.001f) bgRippleAlpha = bgTargetAlpha;

        if (Math.abs(targetBlurRadius - currentBlurRadius) > 0.01f) {
            currentBlurRadius += (targetBlurRadius - currentBlurRadius) * 8f * dt;
            int idx = Math.min(BLUR_CACHE.length - 1, Math.max(0, (int) (currentBlurRadius * 4f) - 1));
            BlurMaskFilter filter = (currentBlurRadius >= 0.25f) ? BLUR_CACHE[idx] : null;
            if (filter != lastAppliedFilter) {
                lastAppliedFilter = filter;
                setBlurFilter(filter);
            }
        } else if (currentBlurRadius != targetBlurRadius) {
            currentBlurRadius = targetBlurRadius;
            int idx = Math.min(BLUR_CACHE.length - 1, Math.max(0, (int) (currentBlurRadius * 4f) - 1));
            BlurMaskFilter filter = (currentBlurRadius >= 0.25f) ? BLUR_CACHE[idx] : null;
            if (filter != lastAppliedFilter) {
                lastAppliedFilter = filter;
                setBlurFilter(filter);
            }
        }

        float remainingDt = dt;
        if (staggerDelay > 0f) {
            if (staggerDelay >= remainingDt) {
                staggerDelay -= remainingDt;
                remainingDt = 0f;
            } else {
                remainingDt -= staggerDelay;
                staggerDelay = 0f;
            }
        }

        if (remainingDt > 0f) {
            if (Math.abs(staggerY) > 0.005f || Math.abs(staggerVy) > 0.005f) {
                float stepDt = remainingDt * 0.25f;
                float damping = 2f * dampingRatio * (float) Math.sqrt(stiffness);
                for (int step = 0; step < 4; step++) {
                    float accel = (-stiffness * staggerY) - (damping * staggerVy);
                    staggerVy += accel * stepDt;
                    staggerY += staggerVy * stepDt;
                }
                if (Math.abs(staggerY) <= 0.005f && Math.abs(staggerVy) <= 0.005f) {
                    staggerY = 0f;
                    staggerVy = 0f;
                }
            } else {
                staggerY = 0f;
                staggerVy = 0f;
            }
        }
    }

    public void updateDynamicHeights(float density) {
        float h = 0;
        float topBotPad = 2 * density;
        if (dotsView != null) h += rawDotsHeight * activeProgress;
        if (mainLineView != null) h += topBotPad + rawMainHeight;
        if (romajiView != null) {
            h += (rawRomajiHeight * activeProgress) - (12 * density * activeProgress);
        }
        if (bgLineView != null) {
            if (mainLineView != null || romajiView != null) h += 6 * density * activeProgress;
            h += rawBgHeight * activeProgress;
        }
        if (mainLineView != null) h += topBotPad;
        calculatedHeight = h;
    }

    public void draw(Canvas canvas, int globalPaddingLeft, float density, int availableWidth) {
        if (calculatedHeight <= 0) return;
        int contentWidth = availableWidth - globalPaddingLeft;
        boolean visuallyRight = isVisuallyRightAligned();
        paddingLeft = (int) (contentWidth * (visuallyRight ? 0.2f : 0.05f));
        paddingRight = (int) (contentWidth * (visuallyRight ? 0.05f : 0.2f));
        int textWidth = contentWidth - paddingLeft - paddingRight;
        prep(textWidth);
        int saveCount = canvas.save();
        float currentY = topY + staggerY;
        float pivotX = visuallyRight ? textWidth : 0f;
        float globalScale = 1.0f + 0.015f * activeProgress;
        canvas.scale(globalScale, globalScale, globalPaddingLeft + availableWidth / 2f, currentY + calculatedHeight / 2f);
        float dotsP = Math.max(0f, activeProgress);
        float bgP = Math.max(0f, activeProgress);
        float romajiP = Math.max(0f, activeProgress);
        float currentDotsH = rawDotsHeight * dotsP;
        float currentBgH = rawBgHeight * bgP;
        float currentRomajiH = rawRomajiHeight * romajiP;
        float mainY = currentY + currentDotsH + (2 * density);
        float romajiY = mainY + rawMainHeight;
        if (romajiView != null) romajiY -= (12 * density);
        float bgY = romajiY + currentRomajiH;
        if (bgLineView != null && (mainLineView != null || (romajiView != null && currentRomajiH > 0))) {
            bgY += 6 * density * bgP;
        }

        if (mainRippleAlpha > 0.001f) {
            ripplePaint.setColor(currentColor);
            ripplePaint.setAlpha((int) (32 * mainRippleAlpha));
            float rBot = (romajiView != null && currentRomajiH > 0) ? (romajiY + rawRomajiHeight) : (mainY + rawMainHeight);
            canvas.drawRoundRect(globalPaddingLeft + (paddingLeft / 2f), mainY, availableWidth - (paddingRight / 2f), rBot, 12 * density, 12 * density, ripplePaint);
        }
        if (bgRippleAlpha > 0.001f && bgLineView != null) {
            ripplePaint.setColor(currentColor);
            ripplePaint.setAlpha((int) (18 * bgRippleAlpha));
            float rBot = bgY + rawBgHeight;
            float maxW = bgLineView.getMaxLineWidth();
            float rW = maxW + 24 * density;
            float rLeft, rRight;
            if (visuallyRight) {
                rRight = globalPaddingLeft + paddingLeft + textWidth + (paddingRight / 2f);
                rLeft = rRight - rW;
            } else {
                rLeft = globalPaddingLeft + (paddingLeft / 2f);
                rRight = rLeft + rW;
            }
            canvas.drawRoundRect(rLeft, bgY, rRight, rBot, 12 * density, 12 * density, ripplePaint);
        }

        if (mainLineView != null) {
            canvas.save();
            canvas.translate(globalPaddingLeft + paddingLeft, mainY);
            float mainScaleFactor = 1.0f + (0.02f * activeProgress);
            canvas.scale(mainScaleFactor, mainScaleFactor, pivotX, rawMainHeight / 2f);
            float mainAlphaFloat = 0.35f + 0.65f * activeProgress;
            mainLineView.setAlpha(mainAlphaFloat);
            mainLineView.draw(canvas);
            canvas.restore();
        }
        if (romajiView != null && currentRomajiH > 0) {
            canvas.save();
            float slideY = romajiY - (rawRomajiHeight * (1f - romajiP));
            canvas.translate(globalPaddingLeft + paddingLeft, slideY);
            float scale = 0.7f + 0.3f * romajiP;
            canvas.scale(scale, scale, pivotX, 0f);
            float romajiAlpha = Math.max(0f, (romajiP - 0.4f) / 0.6f);
            if (romajiAlpha > 0.01f) {
                romajiView.setAlpha(romajiAlpha);
                romajiView.draw(canvas);
            }
            canvas.restore();
        }
        if (bgLineView != null && currentBgH > 0) {
            canvas.save();
            float slideY = bgY - (rawBgHeight * (1f - bgP));
            canvas.translate(globalPaddingLeft + paddingLeft, slideY);
            float scale = 0.7f + 0.3f * bgP;
            canvas.scale(scale, scale, pivotX, 0f);
            float alphaP = Math.max(0f, (bgP - 0.4f) / 0.6f);
            float bgAlpha = 0.65f * alphaP;
            if (bgAlpha > 0.01f) {
                bgLineView.setAlpha(bgAlpha);
                bgLineView.draw(canvas);
            }
            canvas.restore();
        }
        if (dotsView != null && currentDotsH > 0) {
            canvas.save();
            float slideY = currentY - (rawDotsHeight * (1f - dotsP));
            canvas.translate(globalPaddingLeft + paddingLeft * 1.25f, slideY);
            float scale = 0.7f + 0.3f * dotsP;
            canvas.scale(scale, scale, textWidth / 2f, rawDotsHeight / 2f);
            float alphaP = Math.max(0f, (dotsP - 0.4f) / 0.6f);
            if (alphaP > 0.01f) {
                dotsView.setAlpha(alphaP);
                dotsView.draw(canvas);
            }
            canvas.restore();
        }
        canvas.restoreToCount(saveCount);
    }

    public void setTop(float y) {
        this.topY = y;
    }

    public float getTop() {
        return topY;
    }

    public float getBottom() {
        return topY + calculatedHeight;
    }

    public float getLiveHeight() {
        return calculatedHeight;
    }

    public float getInactiveRestingHeight(float density) {
        if (mainLineView != null) {
            return (4 * density) + rawMainHeight;
        }
        return 0f;
    }

    public float getTargetRelativeCenterY(float density) {
        if (item != null && item.mainLine != null && item.mainLine.isWaitingDots) {
            return rawDotsHeight / 2f;
        } else if (mainLineView == null && bgLineView != null) {
            return (2 * density) + (rawBgHeight / 2f);
        }
        return (2 * density) + (rawMainHeight / 2f);
    }

    public LyricItem getItem() {
        return item;
    }

    public void setLyricColor(int color) {
        this.currentColor = color;
        if (dotsView != null) dotsView.setPrimaryColor(color);
        if (mainLineView != null) mainLineView.setLyricColor(color);
        if (bgLineView != null) bgLineView.setLyricColor(color);
        if (romajiView != null) romajiView.setLyricColor(color);
    }

    public void setFontConfig(String config) {
        if (mainLineView != null) mainLineView.setFontConfig(config);
        if (bgLineView != null) bgLineView.setFontConfig(config);
        if (romajiView != null) romajiView.setFontConfig(config);
        invalidateMetrics();
    }

    public void setTextSize(float textSizeDp) {
        if (mainLineView != null) mainLineView.setTextSize(textSizeDp);
        if (bgLineView != null) bgLineView.setTextSize(textSizeDp > 0 ? textSizeDp * 0.75f : -1f);
        if (romajiView != null) romajiView.setTextSize(textSizeDp > 0 ? textSizeDp * 0.5f : -1f);
        invalidateMetrics();
    }

    public void setUseSystemFont(boolean useSystemFont) {
        if (mainLineView != null) mainLineView.setUseSystemFont(useSystemFont);
        if (bgLineView != null) bgLineView.setUseSystemFont(useSystemFont);
        if (romajiView != null) romajiView.setUseSystemFont(useSystemFont);
        invalidateMetrics();
    }

    public void setEnableSparkles(boolean enableSparkles) {
        if (mainLineView != null) mainLineView.setEnableSparkles(enableSparkles);
        if (bgLineView != null) bgLineView.setEnableSparkles(enableSparkles);
        if (romajiView != null) romajiView.setEnableSparkles(enableSparkles);
    }

    public void setBlurFilter(BlurMaskFilter filter) {
        if (mainLineView != null) mainLineView.setBlurFilter(filter);
        if (bgLineView != null) bgLineView.setBlurFilter(filter);
        if (romajiView != null) romajiView.setBlurFilter(filter);
    }

    public int getPartAt(float y, float density) {
        float relativeY = y - (topY + staggerY);
        if (relativeY < 0 || relativeY > calculatedHeight) return 0;

        if (activeProgress < 0.1f || (bgLineView == null && dotsView == null)) return 1;

        float p = Math.max(0f, activeProgress);
        float currentDotsH = rawDotsHeight * p;
        float mainY = currentDotsH + (2 * density);
        float romajiY = mainY + rawMainHeight;
        if (romajiView != null) romajiY -= (12 * density);
        float currentRomajiH = rawRomajiHeight * p;
        float bgY = romajiY + currentRomajiH;
        if (bgLineView != null && (mainLineView != null || (romajiView != null && currentRomajiH > 0))) {
            bgY += 6 * density * p;
        }

        if (relativeY < bgY) {
            return 1;
        } else {
            return 2;
        }
    }

    public void setPressed(int part, boolean pressed) {
        if (part == 1) mainPressed = pressed;
        else if (part == 2) bgPressed = pressed;
        else {
            mainPressed = false;
            bgPressed = false;
        }
    }

    public float getActiveProgress() {
        return activeProgress;
    }

    public void setTargetBlurRadius(float radius) {
        this.targetBlurRadius = radius;
    }

    public float getTargetBlurRadius() {
        return targetBlurRadius;
    }

    public boolean isStillActiveOrFading() {
        if (isActive) return true;
        if (activeProgress > 0.01f) return true;
        if (mainLineView != null && !mainLineView.isColorTransitionFinished()) return true;
        return false;
    }
}

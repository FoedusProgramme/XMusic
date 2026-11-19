package com.xapps.media.xmusic.ui.lyrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import com.xapps.media.xmusic.models.LyricLine;
import java.util.ArrayList;
import java.util.List;

public class XLyricsBidiLineView extends XLyricsLineView {

    private final List<BidiSegmentRun> segmentRuns = new ArrayList<>();
    private final Matrix shaderMatrix = new Matrix();
    private final Paint dstInPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private LinearGradient ltrMaskShader;
    private LinearGradient rtlMaskShader;
    private static final float BIDI_GRADIENT_WIDTH = 90f;

    private static class BidiSegmentRun {
        int start;
        int end;
        int lineIdx;
        boolean isRtl;
        float visualLeft;
        float visualRight;
        float width;
        float logicalStart;
        float logicalEnd;
    }

    protected static class BidiLayoutCache extends LayoutCache {
        List<BidiSegmentRun> segmentRuns = new ArrayList<>();
    }

    public XLyricsBidiLineView(Context context) {
        super(context);
        initBidi();
    }

    public XLyricsBidiLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initBidi();
    }

    private void initBidi() {
        dstInPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        int activeMask = 0xFFFFFFFF;
        int futureMask = 0x38FFFFFF;

        ltrMaskShader = new LinearGradient(
                0, 0, BIDI_GRADIENT_WIDTH, 0,
                new int[]{activeMask, activeMask, futureMask, futureMask},
                new float[]{0f, 0.15f, 0.85f, 1f},
                Shader.TileMode.CLAMP
        );
        rtlMaskShader = new LinearGradient(
                0, 0, BIDI_GRADIENT_WIDTH, 0,
                new int[]{futureMask, futureMask, activeMask, activeMask},
                new float[]{0f, 0.15f, 0.85f, 1f},
                Shader.TileMode.CLAMP
        );
    }

    private static int getCharScriptDirection(CharSequence text, int index) {
        char c = text.charAt(index);
        if (Character.isWhitespace(c)) return 0;
        byte dir = Character.getDirectionality(c);
        if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE) {
            return -1;
        }
        if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT
                || dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING
                || dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE) {
            return 1;
        }
        return 0;
    }

    private static boolean resolveRunRtl(StaticLayout layout, String content, int index) {
        int dir = getCharScriptDirection(content, index);
        if (dir == -1) return true;
        if (dir == 1) return false;

        int lineIdx = layout.getLineForOffset(index);
        for (int i = index - 1; i >= 0 && layout.getLineForOffset(i) == lineIdx; i--) {
            int d = getCharScriptDirection(content, i);
            if (d != 0) return d == -1;
        }
        for (int i = index + 1; i < content.length() && layout.getLineForOffset(i) == lineIdx; i++) {
            int d = getCharScriptDirection(content, i);
            if (d != 0) return d == -1;
        }
        return layout.isRtlCharAt(index);
    }

    private void buildTightDirectionalRuns(StaticLayout layout, LyricLine lyricLine, List<BidiSegmentRun> outRuns, float[] outCharXMap) {
        String content = lyricLine.line.toString();
        int len = content.length();
        Path charPath = new Path();
        RectF charRect = new RectF();
        float currentLogicalX = 0f;

        int start = 0;
        while (start < len) {
            int lineIdx = layout.getLineForOffset(start);
            boolean isRtl = resolveRunRtl(layout, content, start);

            int end = start + 1;
            while (end < len && layout.getLineForOffset(end) == lineIdx) {
                int nextDir = getCharScriptDirection(content, end);
                if (nextDir == 0) {
                    int lookAhead = end + 1;
                    while (lookAhead < len && layout.getLineForOffset(lookAhead) == lineIdx && getCharScriptDirection(content, lookAhead) == 0) {
                        lookAhead++;
                    }
                    if (lookAhead < len && layout.getLineForOffset(lookAhead) == lineIdx) {
                        boolean lookAheadRtl = getCharScriptDirection(content, lookAhead) == -1;
                        if (lookAheadRtl != isRtl) {
                            break;
                        }
                    }
                    end++;
                } else {
                    boolean nextRtl = (nextDir == -1);
                    if (nextRtl != isRtl) {
                        break;
                    }
                    end++;
                }
            }

            int trimmedStart = start;
            int trimmedEnd = end;
            while (trimmedStart < trimmedEnd && Character.isWhitespace(content.charAt(trimmedStart))) {
                trimmedStart++;
            }
            while (trimmedEnd > trimmedStart && Character.isWhitespace(content.charAt(trimmedEnd - 1))) {
                trimmedEnd--;
            }

            float minLeft = Float.MAX_VALUE;
            float maxRight = -Float.MAX_VALUE;

            if (trimmedStart < trimmedEnd) {
                for (int c = trimmedStart; c < trimmedEnd; c++) {
                    charPath.reset();
                    layout.getSelectionPath(c, c + 1, charPath);
                    charPath.computeBounds(charRect, true);
                    if (charRect.width() > 0.1f) {
                        minLeft = Math.min(minLeft, charRect.left);
                        maxRight = Math.max(maxRight, charRect.right);
                    }
                }
            }

            if (minLeft >= maxRight) {
                int sIdx = trimmedStart < trimmedEnd ? trimmedStart : start;
                int eIdx = trimmedStart < trimmedEnd ? trimmedEnd : end;
                float pStart = layout.getPrimaryHorizontal(sIdx);
                float pEnd = (eIdx < len && layout.getLineForOffset(eIdx) == lineIdx)
                        ? layout.getPrimaryHorizontal(eIdx)
                        : (isRtl ? layout.getLineLeft(lineIdx) : layout.getLineRight(lineIdx));
                minLeft = Math.min(pStart, pEnd);
                maxRight = Math.max(pStart, pEnd);
            }

            BidiSegmentRun run = new BidiSegmentRun();
            run.start = start;
            run.end = end;
            run.lineIdx = lineIdx;
            run.isRtl = isRtl;
            run.visualLeft = minLeft - 1.5f;
            run.visualRight = maxRight + 1.5f;
            run.width = Math.max(1f, run.visualRight - run.visualLeft);
            run.logicalStart = currentLogicalX;
            run.logicalEnd = currentLogicalX + run.width;

            int visibleChars = Math.max(1, trimmedEnd - trimmedStart);
            float widthPerVisibleChar = run.width / visibleChars;

            for (int c = start; c < end; c++) {
                if (c <= trimmedStart) {
                    outCharXMap[c] = run.logicalStart;
                } else if (c >= trimmedEnd) {
                    outCharXMap[c] = run.logicalEnd;
                } else {
                    outCharXMap[c] = run.logicalStart + (c - trimmedStart) * widthPerVisibleChar;
                }
            }

            currentLogicalX += run.width;
            outRuns.add(run);
            start = end;
        }
        outCharXMap[len] = currentLogicalX;
    }

    @Override
    public void precomputeLayoutData(String text, int width) {
        if (width <= 0 || getLyricLine() == null) return;

        BidiLayoutCache bidiCache = new BidiLayoutCache();
        bidiCache.text = text;
        bidiCache.width = width;
        bidiCache.alignment = alignment;

        TextPaint paintCopy = new TextPaint(textPaint);
        paintCopy.setFontFeatureSettings("'liga' 1, 'clig' 1");
        paintCopy.setTextAlign(Paint.Align.LEFT);

        boolean startsRtl = LyricItemDelegate.isStartingRtl(getLyricLine().line);
        bidiCache.staticLayout = StaticLayout.Builder.obtain(getLyricLine().line, 0, getLyricLine().line.length(), paintCopy, width)
                .setAlignment(alignment)
                .setTextDirection(startsRtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR)
                .setIncludePad(true)
                .setLineSpacing(0f, 1.2f)
                .build();

        bidiCache.lineStarts = new float[bidiCache.staticLayout.getLineCount()];
        bidiCache.lineWidths = new float[bidiCache.staticLayout.getLineCount()];
        float totW = 0f;
        for (int i = 0; i < bidiCache.staticLayout.getLineCount(); i++) {
            bidiCache.lineStarts[i] = totW;
            bidiCache.lineWidths[i] = Math.max(1f, bidiCache.staticLayout.getLineWidth(i));
            totW += bidiCache.lineWidths[i];
        }

        String content = getLyricLine().line.toString();
        int len = content.length();
        if (len == 0) {
            this.layoutCache = bidiCache;
            return;
        }

        float[] tempCharXMap = new float[len + 1];
        buildTightDirectionalRuns(bidiCache.staticLayout, getLyricLine(), bidiCache.segmentRuns, tempCharXMap);
        bidiCache.charXMap = tempCharXMap;
        bidiCache.totalWidth = tempCharXMap[len];

        this.layoutCache = bidiCache;
    }

    @Override
    public void setText(String text, int width) {
        if (width <= 0 || getLyricLine() == null) return;
        textPaint.setFontFeatureSettings("'liga' 1, 'clig' 1");
        textPaint.setTextAlign(Paint.Align.LEFT);

        LayoutCache baseCache = this.layoutCache;
        if (baseCache instanceof BidiLayoutCache && text.equals(baseCache.text) && width == baseCache.width && this.alignment == baseCache.alignment) {
            BidiLayoutCache cache = (BidiLayoutCache) baseCache;
            this.staticLayout = cache.staticLayout;
            this.lineStarts = cache.lineStarts;
            this.lineWidths = cache.lineWidths;
            this.charXMap = cache.charXMap;
            this.totalWidth = cache.totalWidth;
            this.segmentRuns.clear();
            this.segmentRuns.addAll(cache.segmentRuns);
            return;
        }

        boolean startsRtl = LyricItemDelegate.isStartingRtl(getLyricLine().line);
        staticLayout = StaticLayout.Builder.obtain(getLyricLine().line, 0, getLyricLine().line.length(), textPaint, width)
                .setAlignment(alignment)
                .setTextDirection(startsRtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR)
                .setIncludePad(true)
                .setLineSpacing(0f, 1.2f)
                .build();

        lineStarts = new float[staticLayout.getLineCount()];
        lineWidths = new float[staticLayout.getLineCount()];
        for (int i = 0; i < staticLayout.getLineCount(); i++) {
            lineStarts[i] = i == 0 ? 0f : lineStarts[i - 1] + lineWidths[i - 1];
            lineWidths[i] = Math.max(1f, staticLayout.getLineWidth(i));
        }

        segmentRuns.clear();
        String content = getLyricLine().line.toString();
        int len = content.length();
        if (len == 0) return;

        charXMap = new float[len + 1];
        buildTightDirectionalRuns(staticLayout, getLyricLine(), segmentRuns, charXMap);
        totalWidth = charXMap[len];
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (staticLayout == null || segmentRuns.isEmpty() || getLyricLine() == null) return;

        TextPaint layoutPaint = staticLayout.getPaint();
        layoutPaint.setMaskFilter(textPaint.getMaskFilter());
        layoutPaint.clearShadowLayer();
        layoutPaint.setShader(null);

        long now = SystemClock.uptimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (dt > 0.05f) dt = 0.016f;

        boolean animating = false;
        boolean isSimple = getLyricLine().isSimpleLRC || getLyricLine().isRomaji;

        float renderProgressMs = getContinuousProgressMs(now);

        if (isSimple) {
            currentSimpleState += (targetSimpleState - currentSimpleState) * 15f * dt;
            if (Math.abs(targetSimpleState - currentSimpleState) > 0.005f) animating = true;
        } else {
            long finalEnd = getLyricLine().words.isEmpty() ? getLyricLine().time : getLyricLine().words.get(getLyricLine().words.size() - 1).getEndTime();
            long effEnd = (customEndTime != -1) ? customEndTime : finalEnd;
            if (isProgressAdvancing && renderProgressMs >= getLyricLine().time && renderProgressMs <= effEnd) {
                targetGlobalX = computeGlobalXAtTime(renderProgressMs);
                animating = true;
            }
            float stepDt = dt * 0.5f;
            float stiffness = 240f;
            float damping = 30.5f;
            for (int step = 0; step < 2; step++) {
                float displacement = currentGlobalX - targetGlobalX;
                float acceleration = (-stiffness * displacement) - (damping * velocityX);
                velocityX += acceleration * stepDt;
                currentGlobalX += velocityX * stepDt;
            }
            if (Math.abs(currentGlobalX - targetGlobalX) > 0.02f || Math.abs(velocityX) > 0.05f) animating = true;

            colorTransitionState += (targetColorState - colorTransitionState) * 6f * dt;
            if (Math.abs(targetColorState - colorTransitionState) > 0.005f) animating = true;
        }

        int rgbColor = (activeColor & 0x00FFFFFF) == 0 ? 0x00FFFFFF : (activeColor & 0x00FFFFFF);
        int opaqueTextColor = 0xFF000000 | rgbColor;

        int activeAlpha = 255;
        int pastAlpha = Math.min(255, Math.max(0, (int) (255f * (1f - colorTransitionState) + 128f * colorTransitionState)));
        int futureAlpha = 56;

        float extraPad = getExtraPadding();
        float layerBottom = staticLayout.getHeight() + extraPad * 2f;
        int layerId = canvas.saveLayer(-32f, -32f, getWidth() + 32f, layerBottom + 32f, null);

        canvas.save();
        canvas.translate(0, extraPad);
        layoutPaint.setColor(opaqueTextColor);
        staticLayout.draw(canvas);
        canvas.restore();

        if (isSimple) {
            int simpleAlpha;
            if (currentSimpleState < 1f) {
                simpleAlpha = (int) (futureAlpha + (activeAlpha - futureAlpha) * currentSimpleState);
            } else {
                float r = currentSimpleState - 1f;
                simpleAlpha = (int) (activeAlpha * (1f - r) + 128f * r);
            }
            dstInPaint.setShader(null);
            dstInPaint.setColor((Math.min(255, Math.max(0, simpleAlpha)) << 24) | 0x00FFFFFF);
            canvas.drawRect(-32f, -32f, getWidth() + 32f, layerBottom + 32f, dstInPaint);
            canvas.restoreToCount(layerId);
            if (animating) postInvalidateOnAnimation();
            return;
        }

        boolean isFullyPast = currentGlobalX >= totalWidth + BIDI_GRADIENT_WIDTH;
        boolean isFullyFuture = currentGlobalX <= 0f;

        if (isFullyPast || isFullyFuture) {
            int fullAlpha = isFullyPast ? pastAlpha : futureAlpha;
            dstInPaint.setShader(null);
            dstInPaint.setColor((fullAlpha << 24) | 0x00FFFFFF);
            canvas.drawRect(-32f, -32f, getWidth() + 32f, layerBottom + 32f, dstInPaint);
            canvas.restoreToCount(layerId);
            if (animating) postInvalidateOnAnimation();
            return;
        }

        int lineCount = staticLayout.getLineCount();
        for (int line = 0; line < lineCount; line++) {
            float top = line == 0 ? -32f : staticLayout.getLineTop(line) + extraPad;
            float bottom = line == lineCount - 1 ? layerBottom + 32f : staticLayout.getLineBottom(line) + extraPad;

            float lineMinLeft = Float.MAX_VALUE;
            float lineMaxRight = -Float.MAX_VALUE;

            for (int i = 0; i < segmentRuns.size(); i++) {
                BidiSegmentRun run = segmentRuns.get(i);
                if (run.lineIdx != line) continue;
                lineMinLeft = Math.min(lineMinLeft, run.visualLeft);
                lineMaxRight = Math.max(lineMaxRight, run.visualRight);

                if (currentGlobalX >= run.logicalEnd) {
                    dstInPaint.setShader(null);
                    dstInPaint.setColor((pastAlpha << 24) | 0x00FFFFFF);
                } else if (currentGlobalX <= run.logicalStart) {
                    dstInPaint.setShader(null);
                    dstInPaint.setColor((futureAlpha << 24) | 0x00FFFFFF);
                } else {
                    float localProgress = Math.max(0f, Math.min(1f, (currentGlobalX - run.logicalStart) / Math.max(1f, run.width)));
                    float tx;
                    LinearGradient activeShader;

                    if (run.isRtl) {
                        tx = run.visualRight - (run.width + BIDI_GRADIENT_WIDTH) * localProgress;
                        activeShader = rtlMaskShader;
                    } else {
                        tx = (run.visualLeft - BIDI_GRADIENT_WIDTH) + (run.width + BIDI_GRADIENT_WIDTH) * localProgress;
                        activeShader = ltrMaskShader;
                    }

                    shaderMatrix.setTranslate(tx, 0);
                    activeShader.setLocalMatrix(shaderMatrix);
                    dstInPaint.setShader(activeShader);
                    dstInPaint.setColor(0xFFFFFFFF);
                }

                canvas.drawRect(run.visualLeft, top, run.visualRight, bottom, dstInPaint);
            }

            if (lineMinLeft < lineMaxRight) {
                dstInPaint.setShader(null);
                dstInPaint.setColor((futureAlpha << 24) | 0x00FFFFFF);
                if (lineMinLeft > -32f) {
                    canvas.drawRect(-32f, top, lineMinLeft, bottom, dstInPaint);
                }
                if (lineMaxRight < getWidth() + 32f) {
                    canvas.drawRect(lineMaxRight, top, getWidth() + 32f, bottom, dstInPaint);
                }
                float lastRunRight = lineMinLeft;
                for (int gi = 0; gi < segmentRuns.size(); gi++) {
                    BidiSegmentRun gapRun = segmentRuns.get(gi);
                    if (gapRun.lineIdx != line) continue;
                    if (gapRun.visualLeft > lastRunRight + 0.5f) {
                        int gapAlpha = (currentGlobalX >= gapRun.logicalStart) ? pastAlpha : futureAlpha;
                        dstInPaint.setShader(null);
                        dstInPaint.setColor((gapAlpha << 24) | 0x00FFFFFF);
                        canvas.drawRect(lastRunRight, top, gapRun.visualLeft, bottom, dstInPaint);
                    }
                    lastRunRight = Math.max(lastRunRight, gapRun.visualRight);
                }
            }
        }

        canvas.restoreToCount(layerId);

        if (animating) {
            postInvalidateOnAnimation();
        }
    }
}

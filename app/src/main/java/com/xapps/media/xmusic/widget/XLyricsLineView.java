package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.core.content.res.ResourcesCompat;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.models.LyricLine;
import com.xapps.media.xmusic.models.LyricSyllable;
import com.xapps.media.xmusic.models.LyricWord;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

public class XLyricsLineView extends View {

    public final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
    protected StaticLayout staticLayout;
    private LyricLine lyricLine;
    private LinearGradient brushShader;
    private final Matrix shaderMatrix = new Matrix();
	
	private Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
    
    protected int activeColor = 0xFFFFFFFF;
    protected int pastViewColor = 0x80FFFFFF;
    protected int futureColor = 0x26FFFFFF;
    
    protected float targetGlobalX = 0f;
    protected float targetColorState = 0f;
    
    protected float currentGlobalX = 0f;
    protected float colorTransitionState = 0f;
    protected float velocityX = 0f;
    protected long lastFrameTime = 0;
    protected int currentProgressMs = 0;

    protected float targetSimpleState = 0f;
    protected float currentSimpleState = 0f;
    private long customEndTime = -1;
    
    protected float[] lineStarts;
    private float[] lineWidths;
    protected float[] charXMap;
    protected float totalWidth = 0;
    
    private final List<VisualCluster> clusters = new ArrayList<>();
    
    private boolean isForcedActive = false;
    private static final float GRADIENT_WIDTH = 100f;
    private static final float ELEVATION_AMOUNT = 6f;

    private static class VisualCluster {
        int start;
        int end;
        float x;
        float y;
        float width;
        int lineIdx;
        float currentElevation;
        boolean isHeavy;
        int wordStartMs;
        int wordEndMs;
        float normX;
        float intensity;
        boolean disableScale;
    }

    public XLyricsLineView(Context context) {
        super(context);
        init();
    }

    public XLyricsLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        Typeface customFont = ResourcesCompat.getFont(getContext(), R.font.gsans_flex_full);
        textPaint.setTypeface(Typeface.create(customFont, Typeface.BOLD));
        textPaint.setFontFeatureSettings("'liga' 0, 'clig' 0");
        setClickable(false);
        setFocusable(false);
    }
	
	public void setLyricColor(int color) {
		activeColor = color;
        pastViewColor = (color & 0x00FFFFFF) | 0x80000000;
		futureColor = (color & 0x00FFFFFF) | 0x26000000;
		invalidate();
	}
	
	public void setLineGravity(Layout.Alignment alignment) {
		this.alignment = alignment;
	}
	
	public void setFontConfig(String s) {
		textPaint.setFontVariationSettings(s);
	}
    
    public void setTextSize(int dp) {
        textPaint.setTextSize(spToPx(dp));
    }

    public LyricLine getLyricLine() {
        return lyricLine;
    }

    public void setLyricLine(LyricLine line) {
        this.lyricLine = line;
        this.isForcedActive = (line != null && (line.isRomaji));
        if (textPaint != null) textPaint.setShader(null);
        if (line != null) setTextSize((line.isRomaji || line.isBackground) ? (line.isRomaji? 16 : 24) : 32);
    }

    public void setCustomEndTime(long time) {
        this.customEndTime = time;
    }

    public float getExtraPadding() {
        return textPaint.getTextSize() * 0.4f;
    }

    public int getDesiredHeight() {
        return staticLayout != null ? staticLayout.getHeight() + (int)(getExtraPadding() * 2) : 0;
    }

    public void resetProgress() {
        this.currentGlobalX = 0f;
        this.targetGlobalX = 0f;
        this.colorTransitionState = 0f;
        this.targetColorState = 0f;
        this.currentSimpleState = 0f;
        this.targetSimpleState = 0f;
        this.velocityX = 0f;
        this.currentProgressMs = 0;
        for (VisualCluster vc : clusters) {
            vc.currentElevation = 0f;
        }
        invalidate();
    }

    public void setText(String text, int width) {
        if (width <= 0 || lyricLine == null) return;
        staticLayout = StaticLayout.Builder.obtain(lyricLine.line, 0, lyricLine.line.length(), textPaint, width)
                .setAlignment(alignment)
                .setIncludePad(true)
                .setLineSpacing(0f, 1.2f)
                .build();
        
        lineStarts = new float[staticLayout.getLineCount()];
        lineWidths = new float[staticLayout.getLineCount()];
        totalWidth = 0;
        
        for (int i = 0; i < staticLayout.getLineCount(); i++) {
            lineStarts[i] = totalWidth;
            lineWidths[i] = Math.max(1f, staticLayout.getLineWidth(i));
            totalWidth += lineWidths[i];
        }
        
        charXMap = new float[lyricLine.line.length() + 1];
        for (int i = 0; i < charXMap.length; i++) {
            int lineIdx = staticLayout.getLineForOffset(i);
            float lineLeft = staticLayout.getLineLeft(lineIdx);
            charXMap[i] = lineStarts[lineIdx] + (staticLayout.getPrimaryHorizontal(i) - lineLeft);
        }


        clusters.clear();
        String content = lyricLine.line.toString();
        BreakIterator it = BreakIterator.getCharacterInstance();
        it.setText(content);

        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            VisualCluster vc = new VisualCluster();
            vc.start = start;
            vc.end = end;
            vc.lineIdx = staticLayout.getLineForOffset(start);
            int endLineIdx = staticLayout.getLineForOffset(end);
            
            vc.x = staticLayout.getPrimaryHorizontal(start);
            if (vc.lineIdx == endLineIdx) {
                vc.width = staticLayout.getPrimaryHorizontal(end) - vc.x;
            } else {
                vc.width = staticLayout.getLineWidth(vc.lineIdx) - vc.x;
            }
            vc.y = staticLayout.getLineBaseline(vc.lineIdx);
            vc.currentElevation = 0f;
            vc.isHeavy = false;
            vc.disableScale = false;
            clusters.add(vc);
        }

        if (lyricLine.words != null) {
            for (int w = 0; w < lyricLine.words.size(); w++) {
                LyricWord word = lyricLine.words.get(w);
                if (word.syllables == null || word.syllables.isEmpty()) continue;
                
                int wordLen = 0;
                int wordStartTime = word.syllables.get(0).startTime;
                int wordEndTime = word.syllables.get(word.syllables.size() - 1).endTime;
                
                for (int s = 0; s < word.syllables.size(); s++) {
                    wordLen += word.syllables.get(s).text.length();
                }
                
                if (wordLen > 0) {
                    boolean isArabic = false;
                    boolean isCJK = false;
                    
                    String firstSyl = word.syllables.get(0).text;
                    if (firstSyl != null && !firstSyl.isEmpty()) {
                        Character.UnicodeBlock block = Character.UnicodeBlock.of(firstSyl.charAt(0));
                        if (block != null) {
                            String bName = block.toString();
                            if (bName.contains("ARABIC")) {
                                isArabic = true;
                            } else if (bName.contains("CJK") || bName.contains("HIRAGANA") || bName.contains("KATAKANA") || bName.contains("HANGUL")) {
                                isCJK = true;
                            }
                        }
                    }

                    boolean eligible = false;
                    float minMs = 200f;
                    float msRange = 300f;
                    boolean shouldDisableScale = false;

                    if (isCJK) {
                        if (wordLen == 1) eligible = true;
                        minMs = 400f;
                        msRange = 400f;
                    } else if (isArabic) {
                        if (wordLen > 1 && wordLen <= 7) eligible = true;
                        shouldDisableScale = true;
                    } else {
                        if (wordLen > 1 && wordLen <= 7) eligible = true;
                    }

                    if (eligible) {
                        float msPerLetter = (float)(wordEndTime - wordStartTime) / wordLen;
                        
                        if (msPerLetter >= minMs) {
                            int wordStartOffset = word.startIndex + word.syllables.get(0).relStart;
                            int wordEndOffset = wordStartOffset + wordLen;
                            
                            float wStartX = getGlobalXForOffset(wordStartOffset);
                            float wEndX = getGlobalXForOffset(wordEndOffset);
                            float wordWidth = wEndX - wStartX;
                            
                            float intensityRaw = (msPerLetter - minMs) / msRange;
                            float calculatedIntensity = 0.3f + 0.7f * Math.max(0f, Math.min(1f, intensityRaw));
                            
                            for (int c = 0; c < clusters.size(); c++) {
                                VisualCluster vc = clusters.get(c);
                                if (vc.start >= wordStartOffset && vc.end <= wordEndOffset) {
                                    vc.isHeavy = true;
                                    vc.wordStartMs = wordStartTime;
                                    vc.wordEndMs = wordEndTime;
                                    vc.intensity = calculatedIntensity;
                                    vc.disableScale = shouldDisableScale;
                                    
                                    float charCenter = getGlobalXForOffset(vc.start) + (getGlobalXForOffset(vc.end) - getGlobalXForOffset(vc.start)) / 2f;
                                    vc.normX = wordWidth > 0 ? ((charCenter - wStartX) / wordWidth) * 2f - 1f : 0f;
                                }
                            }
                        }
                    }
                }
            }
        }

        brushShader = new LinearGradient(0, 0, GRADIENT_WIDTH, 0,
                new int[]{activeColor, activeColor, futureColor, futureColor},
                new float[]{0f, 0.1f, 0.9f, 1f},
                Shader.TileMode.CLAMP);
                
        requestLayout();
        invalidate();
    }

    private float getGlobalXForOffset(int offset) {
        if (charXMap == null || offset < 0) return 0f;
        if (offset >= charXMap.length) return totalWidth;
        return charXMap[offset];
    }

    public void updateProgress(int progressMs, boolean snap) {
        if (lyricLine == null || staticLayout == null || lyricLine.words.isEmpty()) return;
        this.currentProgressMs = progressMs;

        long finalEndTime = lyricLine.words.isEmpty() ? lyricLine.time : lyricLine.words.get(lyricLine.words.size() - 1).getEndTime();
        long effectiveEndTime = (customEndTime != -1) ? customEndTime : finalEndTime;

        if (lyricLine.isSimpleLRC || isForcedActive) {
            if (progressMs < lyricLine.time) {
                this.targetSimpleState = 0f;
            } else if (progressMs > effectiveEndTime) {
                this.targetSimpleState = 2f;
            } else {
                this.targetSimpleState = 1f;
            }

            if (snap || this.currentGlobalX == 0f) {
                this.currentSimpleState = this.targetSimpleState;
                this.currentGlobalX = totalWidth + GRADIENT_WIDTH * 3f;
            }
            this.targetGlobalX = totalWidth + GRADIENT_WIDTH * 3f;
            invalidate();
            return;
        }

        float globalX = 0;
        boolean found = false;
        float lastLineEndGlobal = 0;
        int lastEndTime = lyricLine.time;

        if (progressMs < lastEndTime) {
            globalX = 0;
            found = true;
        }

        float newTargetColorState = (progressMs > effectiveEndTime) ? 1f : 0f;

        if (!found) {
            for (int i = 0; i < lyricLine.words.size(); i++) {
                LyricWord word = lyricLine.words.get(i);
                for (int j = 0; j < word.syllables.size(); j++) {
                    LyricSyllable syl = word.syllables.get(j);
                    int charIdx = word.startIndex + syl.relStart;
                    
                    float sylGlobalStart = getGlobalXForOffset(charIdx);
                    float sylGlobalEnd = getGlobalXForOffset(charIdx + syl.text.length());

                    if (progressMs < syl.startTime) {
                        float gap = syl.startTime - lastEndTime;
                        if (gap > 0) {
                            float ratio = (float) (progressMs - lastEndTime) / gap;
                            globalX = lastLineEndGlobal + (sylGlobalStart - lastLineEndGlobal) * interpolator.getInterpolation(ratio);
                        } else {
                            globalX = lastLineEndGlobal;
                        }
                        found = true;
                        break;
                    } else if (progressMs >= syl.startTime && progressMs <= syl.endTime) {
                        float ratio = (float) (progressMs - syl.startTime) / Math.max(1, syl.endTime - syl.startTime);
                        globalX = sylGlobalStart + ((sylGlobalEnd - sylGlobalStart) * interpolator.getInterpolation(ratio));
                        found = true;
                        break;
                    }
                    lastEndTime = syl.endTime;
                    lastLineEndGlobal = sylGlobalEnd;
                }
                if (found) break;
            }
        }

        if (!found) globalX = totalWidth + (GRADIENT_WIDTH * 2.5f);
        
        if (snap || this.currentGlobalX == 0f) {
            this.currentGlobalX = globalX;
            this.velocityX = 0f;
            this.colorTransitionState = newTargetColorState;
            
            boolean isLineActive = progressMs >= lyricLine.time && progressMs <= effectiveEndTime;
            float waveRadius = GRADIENT_WIDTH * 0.75f;
            
            for (VisualCluster vc : clusters) {
                float lineLeft = staticLayout.getLineLeft(vc.lineIdx);
                float clusterCenter = lineStarts[vc.lineIdx] + (vc.x - lineLeft) + (vc.width / 2f);
                float targetElev = 0f;
                
                if (isLineActive) {
                    float distance = globalX - clusterCenter;
                    float t = Math.max(0f, Math.min(1f, (distance + waveRadius) / (waveRadius * 2f)));
                    float smoothT = t * t * (3f - 2f * t);
                    targetElev = -ELEVATION_AMOUNT * smoothT;
                }
                vc.currentElevation = targetElev;
            }

        }

        this.targetGlobalX = globalX;
        this.targetColorState = newTargetColorState;
        invalidate();
    }

    private int blendColors(int color1, int color2, float ratio) {
        float inverseRatio = 1f - ratio;
        float a = ((color1 >> 24) & 0xFF) * inverseRatio + ((color2 >> 24) & 0xFF) * ratio;
        float r = ((color1 >> 16) & 0xFF) * inverseRatio + ((color2 >> 16) & 0xFF) * ratio;
        float g = ((color1 >> 8) & 0xFF) * inverseRatio + ((color2 >> 8) & 0xFF) * ratio;
        float b = (color1 & 0xFF) * inverseRatio + (color2 & 0xFF) * ratio;
        return ((int) a << 24) | ((int) r << 16) | ((int) g << 8) | (int) b;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (staticLayout == null || clusters.isEmpty() || lyricLine == null) return;

        long now = android.os.SystemClock.uptimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (dt > 0.05f) dt = 0.016f;

        boolean animating = false;
        boolean isSimple = lyricLine.isSimpleLRC || isForcedActive;

        if (isSimple) {
            currentSimpleState += (targetSimpleState - currentSimpleState) * 15f * dt;
            if (Math.abs(targetSimpleState - currentSimpleState) > 0.005f) animating = true;
        } else {
            colorTransitionState += (targetColorState - colorTransitionState) * 25f * dt;
            if (Math.abs(targetColorState - colorTransitionState) > 0.005f) animating = true;

            float stiffness = 220f;
            float damping = 28f;
            float displacement = currentGlobalX - targetGlobalX;
            float acceleration = (-stiffness * displacement) - (damping * velocityX);
            velocityX += acceleration * dt;
            currentGlobalX += velocityX * dt;
            
            if (Math.abs(displacement) > 0.1f || Math.abs(velocityX) > 0.5f) animating = true;
        }

        int currentPastColor = blendColors(activeColor, pastViewColor, colorTransitionState);

        long finalEndTime = lyricLine.words.isEmpty() ? lyricLine.time : lyricLine.words.get(lyricLine.words.size() - 1).getEndTime();
        long effectiveEndTime = (customEndTime != -1) ? customEndTime : finalEndTime;
        boolean isLineActive = currentProgressMs >= lyricLine.time && currentProgressMs <= effectiveEndTime;
        float waveRadius = GRADIENT_WIDTH * 0.75f;

        for (int i = 0; i < clusters.size(); i++) {
            VisualCluster vc = clusters.get(i);
            float lineLeft = staticLayout.getLineLeft(vc.lineIdx);
            float lineGlobalStart = lineStarts[vc.lineIdx];
            float lineWidth = lineWidths[vc.lineIdx];
            float lineGlobalEnd = lineGlobalStart + lineWidth;
            float clusterCenter = lineGlobalStart + (vc.x - lineLeft) + (vc.width / 2f);

            float targetElev = 0f;
            if (isLineActive && !isSimple) {
                float distance = currentGlobalX - clusterCenter;
                float t = Math.max(0f, Math.min(1f, (distance + waveRadius) / (waveRadius * 2f)));
                float smoothT = t * t * (3f - 2f * t);
                targetElev = -ELEVATION_AMOUNT * smoothT;
            }


            vc.currentElevation += (targetElev - vc.currentElevation) * 16f * dt;
            if (Math.abs(targetElev - vc.currentElevation) > 0.1f) animating = true;

            float scale = 1.0f;
            int shadowColor = 0;
            float spacingShift = 0f;

            if (vc.isHeavy && currentProgressMs >= vc.wordStartMs && currentProgressMs <= vc.wordEndMs) {
                animating = true;
                float p = (currentProgressMs - vc.wordStartMs) / (float) Math.max(1, vc.wordEndMs - vc.wordStartMs);
                
                if (!vc.disableScale) {
                    float baseScale = 1.0f + (0.1f * vc.intensity) * (float)Math.sin(Math.PI * p);
                    float perspective = -(0.08f * vc.intensity) * vc.normX * (float)Math.sin(2 * Math.PI * p);
                    scale = baseScale + perspective;
                    spacingShift = vc.normX * (10f * vc.intensity) * (float)Math.sin(p * Math.PI);
                }
                
                float distanceToCurrent = currentGlobalX - clusterCenter;
                float glowAlphaProgress = Math.max(0f, Math.min(1f, (distanceToCurrent + waveRadius) / waveRadius));
                int shadowAlpha = (int) (255f * vc.intensity * glowAlphaProgress * (float) Math.sin(p * Math.PI));
                shadowColor = ((shadowAlpha & 0xFF) << 24) | (activeColor & 0x00FFFFFF);
            }

            float drawX = vc.x + spacingShift;
            float drawY = vc.y + vc.currentElevation + getExtraPadding();

            if (scale != 1.0f) {
                canvas.save();
                canvas.scale(scale, scale, drawX + vc.width / 2f, drawY);
            }

            if (shadowColor != 0) {
                textPaint.setShadowLayer(12f, 0f, 0f, shadowColor);
            } else {
                textPaint.clearShadowLayer();
            }

            if (isSimple) {
                int drawColor;
                if (currentSimpleState < 1f) {
                    drawColor = blendColors(futureColor, activeColor, currentSimpleState);
                } else {
                    drawColor = blendColors(activeColor, pastViewColor, currentSimpleState - 1f);
                }
                textPaint.setShader(null);
                textPaint.setColor(drawColor);
                canvas.drawText(lyricLine.line, vc.start, vc.end, drawX, drawY, textPaint);
            } else {
                if (currentGlobalX >= lineGlobalEnd + GRADIENT_WIDTH) {
                    textPaint.setShader(null);
                    textPaint.setColor(currentPastColor);
                } else if (currentGlobalX <= lineGlobalStart - GRADIENT_WIDTH) {
                    textPaint.setShader(null);
                    textPaint.setColor(futureColor);
                } else {
                    float localX = currentGlobalX - lineGlobalStart;
                    float progress = Math.min(1f, Math.max(0f, localX / lineWidth));
                    float translate = (lineLeft - GRADIENT_WIDTH) + (lineWidth + GRADIENT_WIDTH) * progress;
                    
                    shaderMatrix.setTranslate(translate, 0);
                    brushShader.setLocalMatrix(shaderMatrix);
                    textPaint.setShader(brushShader);
                    textPaint.setColor(activeColor);
                }
                canvas.drawText(lyricLine.line, vc.start, vc.end, drawX, drawY, textPaint);
            }


            if (scale != 1.0f) {
                canvas.restore();
            }
        }
        
        textPaint.clearShadowLayer();

        if (animating) postInvalidateOnAnimation();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), getDesiredHeight());
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}

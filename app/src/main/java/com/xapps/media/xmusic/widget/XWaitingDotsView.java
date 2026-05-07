package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.PathInterpolator;
import androidx.core.graphics.ColorUtils;

public class XWaitingDotsView extends View {
    
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long startTime, endTime;
    private int currentProgress = 0;
    
    private float actualScale = 1.0f;
    private final PathInterpolator yosEasing = new PathInterpolator(0.75f, 0f, 0.25f, 1f);
    
    private int primaryColor = 0xFFFFFFFF;
    private int inactiveColor = 0x4DFFFFFF;

    public XWaitingDotsView(Context context) {
        super(context);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setPrimaryColor(int color) {
        this.primaryColor = color;
        this.inactiveColor = ColorUtils.setAlphaComponent(color, 76);
        invalidate();
    }

    public void setTimes(long start, long end) {
        this.startTime = start;
        this.endTime = end;
        invalidate();
    }

    public void updateProgress(int progressMs) {
        this.currentProgress = progressMs;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float dotRadius = 5.5f * getResources().getDisplayMetrics().density;
        float spacing = 19f * getResources().getDisplayMetrics().density;
        float startX = dotRadius;
        float centerY = getHeight() / 2f;

        long duration = Math.max(1, endTime - startTime);
        long timeInto = currentProgress - startTime;
        long timeLeft = endTime - currentProgress;

        long exitDuration = 250;
        long readyDuration = 400;
        long fillDuration = duration - exitDuration - readyDuration;
        
        if (fillDuration < 500) {
            exitDuration = duration / 4;
            readyDuration = duration / 4;
            fillDuration = duration - exitDuration - readyDuration;
        }

        float fillProgress = Math.max(0f, Math.min(1f, timeInto / (float) fillDuration));
        float globalAlphaMult = 1.0f;

        if (timeLeft <= exitDuration) {
            float exitP = 1.0f - (Math.max(0f, (float)timeLeft) / exitDuration);
            actualScale = 1.4f - (1.4f * exitP);
            globalAlphaMult = 1.0f - exitP;
        } else if (timeLeft <= exitDuration + readyDuration) {
            float readyP = 1.0f - ((float)(timeLeft - exitDuration) / readyDuration);
            actualScale = 1.0f + (0.4f * yosEasing.getInterpolation(readyP));
        } else {
            float targetCycle = 3000f;
            int cycles = Math.max(1, Math.round(fillDuration / targetCycle));
            float actualCycleTime = (float) fillDuration / cycles;
            
            float cycleProgress = ((float) timeInto % actualCycleTime) / actualCycleTime;
            
            float rawWave = (float) Math.sin(cycleProgress * Math.PI * 2);
            actualScale = 1.0f + (0.15f * rawWave);
        }

        canvas.save();
        canvas.scale(actualScale, actualScale, startX, centerY);

        for (int i = 1; i <= 3; i++) {
            float average = 1f / 3f;
            float beforePadding = (i - 1) * average;
            float segmentProgress = (fillProgress - beforePadding) / average;
            float progressClamped = Math.max(0f, Math.min(1f, segmentProgress));
            
            int blendedColor = ColorUtils.blendARGB(inactiveColor, primaryColor, progressClamped);
            dotPaint.setColor(blendedColor);
            dotPaint.setAlpha((int) (Color.alpha(blendedColor) * globalAlphaMult));
            canvas.drawCircle(startX + (i - 1) * spacing, centerY, dotRadius, dotPaint);
        }
        
        canvas.restore();

        if (currentProgress >= startTime && currentProgress <= endTime) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (int) (60 * getResources().getDisplayMetrics().density));
    }
}

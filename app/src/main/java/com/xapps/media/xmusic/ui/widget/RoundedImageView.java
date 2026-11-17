package com.xapps.media.xmusic.ui.widget;

/*
 * Original author: appbroker
 * Integrated and revamped for XMusic.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.appcompat.widget.AppCompatImageView;

import com.xapps.media.xmusic.R;

import java.util.Arrays;

public class RoundedImageView extends AppCompatImageView {

    public enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_RIGHT,
        BOTTOM_LEFT
    }

    private final float[] radii = new float[8];
    private final Path clipPath = new Path();
    private final RectF rectF = new RectF();

    public RoundedImageView(@NonNull Context context) {
        this(context, null);
    }

    public RoundedImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.RoundedImageView,
                defStyleAttr,
                0
        );

        setAdjustViewBounds(typedArray.getBoolean(R.styleable.RoundedImageView_android_adjustViewBounds, true));

        float radius = typedArray.getDimension(R.styleable.RoundedImageView_radius, 0f);
        float topLeft = typedArray.getDimension(R.styleable.RoundedImageView_topLeftRadius, radius);
        float topRight = typedArray.getDimension(R.styleable.RoundedImageView_topRightRadius, radius);
        float bottomLeft = typedArray.getDimension(R.styleable.RoundedImageView_bottomLeftRadius, radius);
        float bottomRight = typedArray.getDimension(R.styleable.RoundedImageView_bottomRightRadius, radius);

        typedArray.recycle();

        setCornerRadii(topLeft, topRight, bottomRight, bottomLeft);
    }

    private void setCornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        radii[0] = radii[1] = topLeft;
        radii[2] = radii[3] = topRight;
        radii[4] = radii[5] = bottomRight;
        radii[6] = radii[7] = bottomLeft;
        updatePath();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rectF.set(0, 0, w, h);
        updatePath();
    }

    private void updatePath() {
        if (rectF.width() > 0 && rectF.height() > 0) {
            clipPath.reset();
            clipPath.addRoundRect(rectF, radii, Path.Direction.CW);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!clipPath.isEmpty()) {
            canvas.clipPath(clipPath);
        }
        super.onDraw(canvas);
    }

    public void setRadius(@Px int radius) {
        setRadius((float) radius);
    }

    public void setRadius(@Px float radius) {
        Arrays.fill(radii, radius);
        updatePath();
        invalidate();
    }

    public void setRadius(@NonNull Corner corner, @Px int radius) {
        setRadius(corner, (float) radius);
    }

    public void setRadius(@NonNull Corner corner, @Px float radius) {
        switch (corner) {
            case TOP_LEFT -> {
                radii[0] = radius;
                radii[1] = radius;
            }
            case TOP_RIGHT -> {
                radii[2] = radius;
                radii[3] = radius;
            }
            case BOTTOM_RIGHT -> {
                radii[4] = radius;
                radii[5] = radius;
            }
            case BOTTOM_LEFT -> {
                radii[6] = radius;
                radii[7] = radius;
            }
        }
        updatePath();
        invalidate();
    }
}

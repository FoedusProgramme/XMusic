package com.xapps.media.xmusic.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.graphics.shapes.Morph;
import androidx.graphics.shapes.RoundedPolygon;
import androidx.graphics.shapes.Shapes_androidKt;
import com.google.android.material.shape.MaterialShapes;
import com.xapps.media.xmusic.utils.MaterialColorUtils;

public class CustomLoadingIndicator extends View {

    private final Paint mShapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private final Matrix mMatrix = new Matrix();

    private float mMorphProgress = 0f;
    private float mRotationAngle = 0f;
    private float mBaseRotation = 0f;
    private float mStepProgress = 0f;
    private boolean mIsRefreshing = false;

    private int mCurrentIdx = 0;
    private int mTargetIdx = 1;
    private Morph mActiveMorph;

    private SpringAnimation mCycleSpring;
    private SpringAnimation mPullRotationSpring;

    private static final float STEP_ANGLE = 160f;
    private static final float SPRING_SCALE = 1000f;

    private final Runnable mNextCycleRunnable = this::runCycle;

    @SuppressLint("RestrictedApi")
    private static final RoundedPolygon[] SHAPES = {
            MaterialShapes.normalize(MaterialShapes.COOKIE_6, true, new RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.COOKIE_12, true, new RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.PILL, true, new RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.SOFT_BURST, true, new RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.PENTAGON, true, new RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.COOKIE_9, true, new RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.OVAL, true, new RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.SUNNY, true, new RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.COOKIE_4, true, new RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.CLOVER_4, true, new RectF(-1f, -1f, 1f, 1f))
    };

    public CustomLoadingIndicator(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mShapePaint.setColor(MaterialColorUtils.colorPrimary);
        mBgPaint.setColor(MaterialColorUtils.colorPrimaryContainer);
        updateMorph();
        initSprings();
    }

    private void initSprings() {
        mPullRotationSpring = new SpringAnimation(this, new FloatPropertyCompat<CustomLoadingIndicator>("pullRotation") {
            @Override
            public float getValue(CustomLoadingIndicator indicator) {
                return mRotationAngle;
            }

            @Override
            public void setValue(CustomLoadingIndicator indicator, float value) {
                mRotationAngle = value;
                invalidate();
            }
        });
        mPullRotationSpring.setSpring(new SpringForce(0f)
                .setStiffness(480f)
                .setDampingRatio(0.54f));

        mCycleSpring = new SpringAnimation(this, new FloatPropertyCompat<CustomLoadingIndicator>("stepProgress") {
            @Override
            public float getValue(CustomLoadingIndicator indicator) {
                return mStepProgress;
            }

            @Override
            public void setValue(CustomLoadingIndicator indicator, float value) {
                mStepProgress = value;
                float normalized = value / SPRING_SCALE;
                float clampedMorph = Math.max(0f, Math.min(1f, normalized));
                mMorphProgress = Math.max(mMorphProgress, clampedMorph);
                mRotationAngle = mBaseRotation + (normalized * STEP_ANGLE);
                invalidate();
            }
        });
        mCycleSpring.setMinimumVisibleChange(0.5f);
        mCycleSpring.setSpring(new SpringForce(SPRING_SCALE)
                .setStiffness(300f)
                .setDampingRatio(0.6f));

        mCycleSpring.addEndListener((animation, canceled, value, velocity) -> {
            if (canceled || !mIsRefreshing) return;
            mBaseRotation += STEP_ANGLE;
            mRotationAngle = mBaseRotation;
            mCurrentIdx = mTargetIdx;
            mTargetIdx = (mTargetIdx + 1) % SHAPES.length;
            mMorphProgress = 0f;
            mStepProgress = 0f;
            updateMorph();
            invalidate();
            removeCallbacks(mNextCycleRunnable);
            postDelayed(mNextCycleRunnable, 1);
        });
    }

    private void updateMorph() {
        mActiveMorph = new Morph(SHAPES[mCurrentIdx], SHAPES[mTargetIdx]);
    }

    public void setPullProgress(float progress) {
        if (mIsRefreshing) return;
        float nonNegative = Math.max(0f, progress);
        if (nonNegative <= 0.01f) {
            mCurrentIdx = 0;
            mTargetIdx = 1;
            updateMorph();
        }
        mMorphProgress = 0f;
        mPullRotationSpring.animateToFinalPosition(nonNegative * STEP_ANGLE);
        invalidate();
    }

    public void onDetentBroken() {
        if (mIsRefreshing) return;
        mMorphProgress = 0f;
        if (mPullRotationSpring != null) {
            mPullRotationSpring.setStartVelocity(540f);
            mPullRotationSpring.animateToFinalPosition(STEP_ANGLE);
        }
    }

    public void startRefreshing() {
        if (mIsRefreshing) return;
        mIsRefreshing = true;

        removeCallbacks(mNextCycleRunnable);
        if (mPullRotationSpring != null && mPullRotationSpring.isRunning()) {
            mPullRotationSpring.cancel();
        }
        if (mCycleSpring != null && mCycleSpring.isRunning()) {
            mCycleSpring.cancel();
        }

        mBaseRotation = mRotationAngle;
        mCurrentIdx = 0;
        mTargetIdx = 1;
        mMorphProgress = 0f;
        mStepProgress = 0f;
        updateMorph();

        runCycle();
    }

    private void runCycle() {
        if (!mIsRefreshing) return;
        if (mCycleSpring.isRunning()) {
            mCycleSpring.cancel();
        }
        mCycleSpring.setStartValue(mStepProgress);
        mCycleSpring.setStartVelocity(2400f);
        mCycleSpring.animateToFinalPosition(SPRING_SCALE);
    }

    public void stopRefreshing() {
        mIsRefreshing = false;
        removeCallbacks(mNextCycleRunnable);
        if (mCycleSpring != null && mCycleSpring.isRunning()) {
            mCycleSpring.cancel();
        }
        if (mPullRotationSpring != null && mPullRotationSpring.isRunning()) {
            mPullRotationSpring.cancel();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mNextCycleRunnable);
        if (mCycleSpring != null && mCycleSpring.isRunning()) {
            mCycleSpring.cancel();
        }
        if (mPullRotationSpring != null && mPullRotationSpring.isRunning()) {
            mPullRotationSpring.cancel();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float rad = Math.min(cx, cy) * 0.85f;

        canvas.drawCircle(cx, cy, rad, mBgPaint);

        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(mRotationAngle);

        float s = rad * 0.65f;
        mPath.rewind();
        Shapes_androidKt.toPath(mActiveMorph, Math.max(0f, Math.min(1f, mMorphProgress)), mPath);
        mMatrix.setScale(s, s);
        mPath.transform(mMatrix);

        canvas.drawPath(mPath, mShapePaint);
        canvas.restore();
    }
}
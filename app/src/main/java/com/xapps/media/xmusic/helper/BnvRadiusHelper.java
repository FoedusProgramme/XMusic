package com.xapps.media.xmusic.helper;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.animation.PathInterpolator;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.shape.MaterialShapeDrawable;

public class BnvRadiusHelper {

    private final BottomNavigationView bnv;
    private MaterialShapeDrawable shapeDrawable;
    private ValueAnimator animator;

    private float currentTl = 0f;
    private float currentTr = 0f;
    private float currentBl = 0f;
    private float currentBr = 0f;

    public BnvRadiusHelper(BottomNavigationView bnv) {
        this.bnv = bnv;
        setupDrawable();
    }

    private void setupDrawable() {
        Drawable bg = bnv.getBackground();
        if (bg instanceof MaterialShapeDrawable) {
            shapeDrawable = (MaterialShapeDrawable) bg;
        } else {
            shapeDrawable = new MaterialShapeDrawable();
            if (bg instanceof ColorDrawable) {
                shapeDrawable.setFillColor(ColorStateList.valueOf(((ColorDrawable) bg).getColor()));
            }
            bnv.setBackground(shapeDrawable);
        }
    }

    public void setCornersInstantly(float tl, float tr, float bl, float br) {
        currentTl = Math.max(0f, tl);
        currentTr = Math.max(0f, tr);
        currentBl = Math.max(0f, bl);
        currentBr = Math.max(0f, br);
        applyCorners(currentTl, currentTr, currentBl, currentBr);
    }

    public void animateTopCorners(float radiusPx, long duration) {
        animateCorners(radiusPx, radiusPx, currentBl, currentBr, duration);
    }

    public void animateCorners(float targetTl, float targetTr, float targetBl, float targetBr, long duration) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        float startTl = currentTl;
        float startTr = currentTr;
        float startBl = currentBl;
        float startBr = currentBr;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f));
        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            currentTl = startTl + (targetTl - startTl) * fraction;
            currentTr = startTr + (targetTr - startTr) * fraction;
            currentBl = startBl + (targetBl - startBl) * fraction;
            currentBr = startBr + (targetBr - startBr) * fraction;
            applyCorners(currentTl, currentTr, currentBl, currentBr);
        });
        animator.start();
    }

    private void applyCorners(float tl, float tr, float bl, float br) {
        if (shapeDrawable == null) return;
        shapeDrawable.setShapeAppearanceModel(
            shapeDrawable.getShapeAppearanceModel().toBuilder()
                .setTopLeftCornerSize(tl)
                .setTopRightCornerSize(tr)
                .setBottomLeftCornerSize(bl)
                .setBottomRightCornerSize(br)
                .build()
        );
    }
}

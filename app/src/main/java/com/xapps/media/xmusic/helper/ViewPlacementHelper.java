package com.xapps.media.xmusic.helper;

import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;

public class ViewPlacementHelper {

    public enum Vertical {
        TOP_TO_TOP, BOTTOM_TO_BOTTOM, TOP_TO_BOTTOM, BOTTOM_TO_TOP, NONE
    }

    public enum Horizontal {
        START_TO_START, END_TO_END, START_TO_END, END_TO_START, NONE
    }

    private final View target;
    private final View anchor;
    
    private Vertical verticalConstraint = Vertical.NONE;
    private Horizontal horizontalConstraint = Horizontal.NONE;

    private int verticalMargin = 0;
    private int horizontalMargin = 0;
    
    private int targetWidth = Integer.MIN_VALUE;
    private int targetHeight = Integer.MIN_VALUE;

    private final int[] anchorLoc = new int[2];
    private final int[] targetLoc = new int[2];

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        updatePlacement();
        return true;
    };

    public ViewPlacementHelper(@NonNull View target, @NonNull View anchor) {
        this.target = target;
        this.anchor = anchor;
    }

    public ViewPlacementHelper setVerticalConstraint(Vertical constraint, int marginPx) {
        this.verticalConstraint = constraint;
        this.verticalMargin = marginPx;
        return this;
    }

    public ViewPlacementHelper setHorizontalConstraint(Horizontal constraint, int marginPx) {
        this.horizontalConstraint = constraint;
        this.horizontalMargin = marginPx;
        return this;
    }

    public ViewPlacementHelper setWidth(int width) {
        this.targetWidth = width;
        return this;
    }

    public ViewPlacementHelper setHeight(int height) {
        this.targetHeight = height;
        return this;
    }

    public void attach() {
        target.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        if (target.isLaidOut() && anchor.isLaidOut()) {
            updatePlacement();
        }
    }

    public void detach() {
        target.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
    }

    private void updatePlacement() {
        if (target == null || anchor == null || target.getVisibility() == View.GONE || anchor.getVisibility() == View.GONE) {
            return;
        }

        boolean needsLayout = false;

        if (targetWidth != Integer.MIN_VALUE && target.getWidth() != targetWidth) {
            target.getLayoutParams().width = targetWidth;
            needsLayout = true;
        }

        if (targetHeight != Integer.MIN_VALUE && target.getHeight() != targetHeight) {
            target.getLayoutParams().height = targetHeight;
            needsLayout = true;
        }

        if (needsLayout) {
            target.requestLayout();
            return;
        }

        anchor.getLocationInWindow(anchorLoc);
        float currentTransX = target.getTranslationX();
        float currentTransY = target.getTranslationY();
        
        target.getLocationInWindow(targetLoc);
        float baseTargetX = targetLoc[0] - currentTransX;
        float baseTargetY = targetLoc[1] - currentTransY;

        float targetW = target.getWidth();
        float targetH = target.getHeight();
        float anchorW = anchor.getWidth();
        float anchorH = anchor.getHeight();

        float anchorX = anchorLoc[0];
        float anchorY = anchorLoc[1];

        float newTransX = currentTransX;
        float newTransY = currentTransY;

        if (verticalConstraint != Vertical.NONE) {
            float targetY = 0;
            if (verticalConstraint == Vertical.TOP_TO_TOP) {
                targetY = anchorY + verticalMargin;
            } else if (verticalConstraint == Vertical.BOTTOM_TO_BOTTOM) {
                targetY = anchorY + anchorH - targetH - verticalMargin;
            } else if (verticalConstraint == Vertical.TOP_TO_BOTTOM) {
                targetY = anchorY + anchorH + verticalMargin;
            } else if (verticalConstraint == Vertical.BOTTOM_TO_TOP) {
                targetY = anchorY - targetH - verticalMargin;
            }
            newTransY = targetY - baseTargetY;
        }

        if (horizontalConstraint != Horizontal.NONE) {
            float targetX = 0;
            if (horizontalConstraint == Horizontal.START_TO_START) {
                targetX = anchorX + horizontalMargin;
            } else if (horizontalConstraint == Horizontal.END_TO_END) {
                targetX = anchorX + anchorW - targetW - horizontalMargin;
            } else if (horizontalConstraint == Horizontal.START_TO_END) {
                targetX = anchorX + anchorW + horizontalMargin;
            } else if (horizontalConstraint == Horizontal.END_TO_START) {
                targetX = anchorX - targetW - horizontalMargin;
            }
            newTransX = targetX - baseTargetX;
        }

        if (Math.abs(currentTransX - newTransX) > 0.5f) {
            target.setTranslationX(newTransX);
        }
        if (Math.abs(currentTransY - newTransY) > 0.5f) {
            target.setTranslationY(newTransY);
        }
    }
}

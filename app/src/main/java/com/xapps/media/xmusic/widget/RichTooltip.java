package com.xapps.media.xmusic.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.EdgeTreatment;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.ShapePath;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.utils.MaterialColorUtils;

public class RichTooltip {
    private static final int ICON_SIZE_DP = 18;

    public enum TooltipStyle { PRIMARY, SECONDARY, TERTIARY }
    public enum ButtonStyle { PRIMARY, TONAL, OUTLINED, TEXT, TERTIARY }

    private static final int RADIUS_DP = 16;
    private static final int CARET_SIZE_DP = 12;
    private static final int TITLE_SP = 18;
    private static final int MESSAGE_SP = 14;
    private static final int ANCHOR_MARGIN_DP = 8;
    private static final int SCREEN_MARGIN_DP = 16;

    private final PopupWindow popupWindow;
    private final Context context;
    private final LinearLayout root;
    private final int bgColor;
    private final int caretSizePx;

    private boolean isDismissing = false;
    private View anchor;
    private View.OnAttachStateChangeListener attachStateChangeListener;

    @SuppressLint("ClickableViewAccessibility")
    private RichTooltip(Builder builder) {
        this.context = builder.context;
        this.root = new LinearLayout(context);
        this.root.setOrientation(LinearLayout.VERTICAL);

        this.caretSizePx = dpToPx(context, CARET_SIZE_DP);

        if (builder.style == TooltipStyle.SECONDARY) {
            this.bgColor = MaterialColorUtils.colorOnSecondaryContainer;
        } else if (builder.style == TooltipStyle.TERTIARY) {
            this.bgColor = MaterialColorUtils.colorOnTertiaryContainer;
        } else {
            this.bgColor = MaterialColorUtils.colorOnPrimaryContainer;
        }

        int textColor = MaterialColorUtils.colorOnPrimary;
        if (builder.style == TooltipStyle.SECONDARY) {
            textColor = MaterialColorUtils.colorOnSecondary;
        } else if (builder.style == TooltipStyle.TERTIARY) {
            textColor = MaterialColorUtils.colorOnTertiary;
        }

        if (builder.title != null) {
            TextView titleView = new TextView(context);
            titleView.setText(builder.title);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP);
            titleView.setTextColor(textColor);
            titleView.setTypeface(ResourcesCompat.getFont(titleView.getContext(), R.font.google_sans_flex), android.graphics.Typeface.BOLD);
            titleView.setGravity(Gravity.CENTER_VERTICAL);

            if (builder.iconRes != 0) {
                Drawable icon = ContextCompat.getDrawable(context, builder.iconRes);
                if (icon != null) {
                    icon.setTint(textColor);
                    int iconSize = dpToPx(context, ICON_SIZE_DP);
                    icon.setBounds(0, 0, iconSize, iconSize);
                    titleView.setCompoundDrawables(icon, null, null, null);
                    titleView.setCompoundDrawablePadding(dpToPx(context, 8));
                }
            }
            root.addView(titleView);
        }

        TextView messageView = new TextView(context);
        messageView.setText(builder.message);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_SP);
        messageView.setTextColor(textColor);

        if (builder.title != null) {
            messageView.setPadding(0, dpToPx(context, 4), 0, 0);
        }
        root.addView(messageView);

        LinearLayout buttonContainer = new LinearLayout(context);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setGravity(Gravity.END);
        buttonContainer.setPadding(0, dpToPx(context, 12), 0, 0);

        if (builder.btn2Text != null) {
            MaterialButton btn2 = createButton(context, builder.btn2Text, builder.btn2Style, textColor, builder.btn2Listener);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dpToPx(context, 8));
            buttonContainer.addView(btn2, params);
        }

        if (builder.btn1Text != null) {
            MaterialButton btn1 = createButton(context, builder.btn1Text, builder.btn1Style, textColor, builder.btn1Listener);
            buttonContainer.addView(btn1);
        }

        if (builder.btn1Text != null || builder.btn2Text != null) {
            root.addView(buttonContainer);
        }

        popupWindow = new PopupWindow(root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true) {
            @Override
            public void dismiss() {
                if (!builder.cancelable && !isDismissing) {
                    return;
                }

                if (!isDismissing) {
                    RichTooltip.this.dismiss();
                } else {
                    super.dismiss();
                }
            }
        };

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);

        popupWindow.setTouchInterceptor((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                if (builder.cancelable) {
                    RichTooltip.this.dismiss();
                }
                return true;
            }
            return false;
        });
    }

    public void show(View anchor) {
        if (isDismissing || popupWindow.isShowing()) return;

        if (anchor.getWidth() == 0 || anchor.getHeight() == 0) {
            anchor.post(() -> show(anchor));
            return;
        }

        this.anchor = anchor;

        int[] location = new int[2];
        anchor.getLocationInWindow(location);
        int anchorX = location[0];
        int anchorY = location[1];
        int anchorWidth = anchor.getWidth();
        int anchorHeight = anchor.getHeight();

        View rootView = anchor.getRootView();
        int windowWidth = rootView.getWidth();
        int windowHeight = rootView.getHeight();

        int margin = dpToPx(context, ANCHOR_MARGIN_DP);
        int screenMargin = dpToPx(context, SCREEN_MARGIN_DP);
        int cornerRadius = dpToPx(context, RADIUS_DP);

        int paddingH = dpToPx(context, 16);
        int paddingV = dpToPx(context, 12);
        int maxTooltipWidth = dpToPx(context, 320);

        root.setPadding(paddingH, paddingV + caretSizePx, paddingH, paddingV + caretSizePx);
        root.measure(
                View.MeasureSpec.makeMeasureSpec(maxTooltipWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.UNSPECIFIED
        );
        int tooltipWidth = root.getMeasuredWidth();
        int tooltipHeight = root.getMeasuredHeight();

        int anchorCenterX = anchorX + (anchorWidth / 2);
        int anchorCenterY = anchorY + (anchorHeight / 2);

        boolean showAbove = anchorCenterY > (windowHeight / 2);

        int popupX = anchorCenterX - (tooltipWidth / 2);

        if (popupX < screenMargin) {
            popupX = screenMargin;
        } else if (popupX + tooltipWidth > windowWidth - screenMargin) {
            popupX = windowWidth - tooltipWidth - screenMargin;
        }

        float relativeCaretX = anchorCenterX - popupX;
        float minCaretPos = cornerRadius + caretSizePx;
        float maxCaretPos = tooltipWidth - cornerRadius - caretSizePx;

        if (relativeCaretX < minCaretPos) relativeCaretX = minCaretPos;
        if (relativeCaretX > maxCaretPos) relativeCaretX = maxCaretPos;

        ShapeAppearanceModel.Builder shapeBuilder = ShapeAppearanceModel.builder()
                .setAllCorners(CornerFamily.ROUNDED, cornerRadius);

        SmoothCaretEdgeTreatment caretTreatment = new SmoothCaretEdgeTreatment(
                caretSizePx, relativeCaretX, cornerRadius, tooltipWidth, showAbove
        );

        InsetDrawable insetDrawable;

        if (showAbove) {
            shapeBuilder.setBottomEdge(caretTreatment);
            MaterialShapeDrawable background = new MaterialShapeDrawable(shapeBuilder.build());
            background.setFillColor(ColorStateList.valueOf(bgColor));
            insetDrawable = new InsetDrawable(background, 0, 0, 0, caretSizePx);
        } else {
            shapeBuilder.setTopEdge(caretTreatment);
            MaterialShapeDrawable background = new MaterialShapeDrawable(shapeBuilder.build());
            background.setFillColor(ColorStateList.valueOf(bgColor));
            insetDrawable = new InsetDrawable(background, 0, caretSizePx, 0, 0);
        }

        root.setBackground(insetDrawable);
        if (showAbove) {
            root.setPadding(paddingH, paddingV, paddingH, paddingV + caretSizePx);
        } else {
            root.setPadding(paddingH, paddingV + caretSizePx, paddingH, paddingV);
        }

        root.measure(
                View.MeasureSpec.makeMeasureSpec(tooltipWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
        );
        tooltipHeight = root.getMeasuredHeight();

        int popupY;
        if (showAbove) {
            popupY = anchorY - tooltipHeight - margin;
        } else {
            popupY = anchorY + anchorHeight + margin;
        }

        popupWindow.setWidth(tooltipWidth);
        popupWindow.setHeight(tooltipHeight);
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, popupX, popupY);

        root.setPivotX(relativeCaretX);
        root.setPivotY(showAbove ? tooltipHeight : 0);

        root.setAlpha(0f);
        root.setScaleX(0.8f);
        root.setScaleY(0.8f);

        root.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        attachStateChangeListener = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {}

            @Override
            public void onViewDetachedFromWindow(View v) {
                dismiss();
            }
        };
        this.anchor.addOnAttachStateChangeListener(attachStateChangeListener);
    }

    public void dismiss() {
        if (isDismissing) return;
        isDismissing = true;

        if (anchor != null && attachStateChangeListener != null) {
            anchor.removeOnAttachStateChangeListener(attachStateChangeListener);
        }

        root.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(120)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    popupWindow.dismiss();
                    isDismissing = false;
                    anchor = null;
                })
                .start();
    }

    private MaterialButton createButton(Context context, String text, ButtonStyle style, int color, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(context);
        button.setText(text);

        if (style == ButtonStyle.OUTLINED) {
            button.setStrokeWidth(dpToPx(context, 1));
            button.setStrokeColor(ColorStateList.valueOf(color));
            button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            button.setTextColor(color);
        } else if (style == ButtonStyle.TEXT) {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            button.setElevation(0);
            button.setTextColor(color);
        } else if (style == ButtonStyle.PRIMARY) {
            button.setBackgroundTintList(ColorStateList.valueOf(MaterialColorUtils.colorPrimaryContainer));
            button.setTextColor(MaterialColorUtils.colorPrimary);
        } else if (style == ButtonStyle.TONAL) {
            button.setBackgroundTintList(ColorStateList.valueOf(MaterialColorUtils.colorSecondaryContainer));
            button.setTextColor(MaterialColorUtils.colorSecondary);
        } else if (style == ButtonStyle.TERTIARY) {
            button.setBackgroundTintList(ColorStateList.valueOf(MaterialColorUtils.colorTertiaryContainer));
            button.setTextColor(MaterialColorUtils.colorTertiary);
        }

        button.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            dismiss();
        });
        return button;
    }

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static class Builder {
        private final Context context;
        private String title;
        private String message;
        private int iconRes = 0;
        private TooltipStyle style = TooltipStyle.PRIMARY;
        private boolean cancelable = true;

        private String btn1Text;
        private ButtonStyle btn1Style = ButtonStyle.TEXT;
        private View.OnClickListener btn1Listener;

        private String btn2Text;
        private ButtonStyle btn2Style = ButtonStyle.TEXT;
        private View.OnClickListener btn2Listener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setIconRes(int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setStyle(TooltipStyle style) {
            this.style = style;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Builder setPrimaryAction(String text, ButtonStyle style, View.OnClickListener listener) {
            this.btn1Text = text;
            this.btn1Style = style;
            this.btn1Listener = listener;
            return this;
        }

        public Builder setSecondaryAction(String text, ButtonStyle style, View.OnClickListener listener) {
            this.btn2Text = text;
            this.btn2Style = style;
            this.btn2Listener = listener;
            return this;
        }

        public RichTooltip build() {
            return new RichTooltip(this);
        }
    }

    private static class SmoothCaretEdgeTreatment extends EdgeTreatment {
        private final float size;
        private final float relativeCaretX;
        private final float cornerRadius;
        private final float totalWidth;
        private final boolean isBottom;

        SmoothCaretEdgeTreatment(float size, float relativeCaretX, float cornerRadius, float totalWidth, boolean isBottom) {
            this.size = size;
            this.relativeCaretX = relativeCaretX;
            this.cornerRadius = cornerRadius;
            this.totalWidth = totalWidth;
            this.isBottom = isBottom;
        }

        @Override
        public void getEdgePath(float length, float center, float interpolation, @NonNull ShapePath shapePath) {
            float edgeX;
            if (isBottom) {
                edgeX = totalWidth - relativeCaretX - cornerRadius;
            } else {
                edgeX = relativeCaretX - cornerRadius;
            }

            float curve = size * 0.35f;
            shapePath.lineTo(edgeX - size, 0);
            shapePath.lineTo(edgeX - curve, -(size - curve));
            shapePath.quadToPoint(edgeX, -size, edgeX + curve, -(size - curve));
            shapePath.lineTo(edgeX + size, 0);
            shapePath.lineTo(length, 0);
        }
    }
}
package com.xapps.media.xmusic.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import java.io.File;

public class SongCover extends MaterialCardView {

    private AppCompatImageView innerImageView;
    private Drawable previousDrawable;
    private ValueAnimator animator;
    private float transitionProgress = 1f;
    private CustomTarget<Drawable> glideTarget;

    public SongCover(Context context) {
        super(context);
        init(context, null);
    }

    public SongCover(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SongCover(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setCardElevation(0f);
        setCardBackgroundColor(Color.TRANSPARENT);
        setStrokeWidth(0);

        innerImageView = new AppCompatImageView(context) {
            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                if (previousDrawable != null) {
                    previousDrawable.setBounds(0, 0, w, h);
                }
            }

            @Override
            protected void onDraw(Canvas canvas) {
                if (transitionProgress < 1f && previousDrawable != null) {
                    canvas.save();
                    float previousAlphaProgress = Math.max(0f, 1f - (transitionProgress * 2f));
                    float previousScale = 1f + (0.1f * transitionProgress);
                    int prevAlpha = (int) (255 * previousAlphaProgress);
                    canvas.scale(previousScale, previousScale, getWidth() / 2f, getHeight() / 2f);
                    previousDrawable.setAlpha(prevAlpha);
                    previousDrawable.draw(canvas);
                    canvas.restore();

                    canvas.save();
                    float currentAlphaProgress = Math.max(0f, (transitionProgress - 0.5f) * 2f);
                    float currentScale = 0.9f + (0.1f * transitionProgress);
                    int currAlpha = (int) (255 * currentAlphaProgress);
                    canvas.scale(currentScale, currentScale, getWidth() / 2f, getHeight() / 2f);
                    if (getDrawable() != null) {
                        getDrawable().mutate().setAlpha(currAlpha);
                    }
                    super.onDraw(canvas);
                    canvas.restore();
                } else {
                    if (getDrawable() != null) {
                        getDrawable().mutate().setAlpha(255);
                    }
                    super.onDraw(canvas);
                }
            }
        };

        innerImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(innerImageView, params);

        if (attrs != null) {
            int[] attrsArray = {android.R.attr.src};
            TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
            Drawable srcDrawable = ta.getDrawable(0);
            if (srcDrawable != null) {
                innerImageView.setImageDrawable(srcDrawable);
            }
            ta.recycle();
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(350);
        animator.setInterpolator(new PathInterpolator(0.4f, 0.0f, 0.2f, 1f));
        animator.addUpdateListener(animation -> {
            transitionProgress = (float) animation.getAnimatedValue();
            innerImageView.invalidate();
        });
    }

    public void load(Uri uri) {
        executeGlideLoad(uri);
    }

    public void load(File file) {
        executeGlideLoad(file);
    }

    public void load(int resourceId) {
        executeGlideLoad(resourceId);
    }

    public void load(String filePath) {
        executeGlideLoad(filePath);
    }

    private void executeGlideLoad(Object model) {
        if (glideTarget != null) {
            Glide.with(getContext()).clear(glideTarget);
        }

        glideTarget = new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                setImage(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                if (placeholder != null) {
                    setImageWithoutAnimation(placeholder);
                }
            }
        };

        Glide.with(getContext())
                .load(model)
                .override(Target.SIZE_ORIGINAL)
                .centerCrop()
                .into(glideTarget);
    }

    public void setImage(Drawable drawable) {
        Drawable current = innerImageView.getDrawable();
        if (current != null && drawable != null && current != drawable) {
            if (current.getConstantState() != null) {
                previousDrawable = current.getConstantState().newDrawable().mutate();
                previousDrawable.setBounds(0, 0, innerImageView.getWidth(), innerImageView.getHeight());
            } else {
                previousDrawable = current;
            }
            transitionProgress = 0f;
            animator.cancel();
            animator.start();
        } else {
            transitionProgress = 1f;
            previousDrawable = null;
        }
        innerImageView.setImageDrawable(drawable);
    }

    public void setImage(Bitmap bitmap) {
        if (bitmap != null) {
            setImage(new BitmapDrawable(getResources(), bitmap));
        } else {
            setImage((Drawable) null);
        }
    }

    public void setImageWithoutAnimation(Drawable drawable) {
        transitionProgress = 1f;
        previousDrawable = null;
        animator.cancel();
        innerImageView.setImageDrawable(drawable);
    }

    public void setImageWithoutAnimation(Bitmap bitmap) {
        if (bitmap != null) {
            setImageWithoutAnimation(new BitmapDrawable(getResources(), bitmap));
        } else {
            setImageWithoutAnimation((Drawable) null);
        }
    }

    public void setCornerRadius(float radius) {
        setRadius(radius);
    }

    public void setScaleType(ImageView.ScaleType scaleType) {
        innerImageView.setScaleType(scaleType);
    }
}

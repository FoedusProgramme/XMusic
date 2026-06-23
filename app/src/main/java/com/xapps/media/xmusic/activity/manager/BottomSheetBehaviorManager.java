package com.xapps.media.xmusic.activity.manager;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import androidx.annotation.NonNull;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.data.LiveColors;
import com.xapps.media.xmusic.databinding.ActivityMainBinding;
import com.xapps.media.xmusic.models.BottomSheetBehavior;
import com.xapps.media.xmusic.utils.ColorPaletteUtils;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.widget.ExpressiveSliderLayout;

public class BottomSheetBehaviorManager {

    private ActivityMainBinding binding;
    private MainActivity activity;

    public BottomSheetBehaviorManager(MainActivity activity) {
        this.activity = activity;
        this.binding = activity.getBinding();
	}

    public void setupCallbacks() {
		binding.expressiveBottomSheet.addSliderCallback(new ExpressiveSliderLayout.SliderCallback() {
			@Override
			public void onStateChanged(int newState) {
                if (binding.lyricsContainer.getVisibility() != View.GONE) return;
                binding.expressiveBottomSheet.getPredictiveBackCallback().setEnabled(!(newState == ExpressiveSliderLayout.STATE_SETTLING || newState == ExpressiveSliderLayout.STATE_DRAGGING));
				if (newState == ExpressiveSliderLayout.STATE_DRAGGING) {
                    activity.innerBottomSheetBehavior.setDraggable(false);
					binding.musicProgress.animate().alpha(0f).setDuration(100).start();
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                     activity.innerBottomSheetBehavior.setDraggable(true);
				} else if (newState == ExpressiveSliderLayout.STATE_COLLAPSED || newState == ExpressiveSliderLayout.STATE_HIDDEN) {
                    activity.helper.animateCorners(XUtils.convertToPx(activity, 4f), XUtils.convertToPx(activity, 4f), XUtils.convertToPx(activity, 18f), XUtils.convertToPx(activity, 18f), 300);
                    binding.fragmentsContainer.setRenderEffect(null);
                    activity.innerBottomSheetBehavior.setDraggable(false);
                    binding.expressiveBottomSheet.getPredictiveBackCallback().setEnabled(false);
                    if (newState == ExpressiveSliderLayout.STATE_HIDDEN) activity.floatBnv(false);
					if (newState == ExpressiveSliderLayout.STATE_HIDDEN) {
                        activity.helper.animateCorners(XUtils.convertToPx(activity, 18f), XUtils.convertToPx(activity, 18f), XUtils.convertToPx(activity, 4f), XUtils.convertToPx(activity, 4f), 300);
                        activity.updateAdapters(-1, false);
                        ColorPaletteUtils.lightColors = null;
                        ColorPaletteUtils.darkColors = null;
                        activity.getController().stop();
                        activity.getController().clearMediaItems();
                        //activity.playerState = activity.UIState.HIDDEN;
						//isBsInvisible = true;
				    } else {
						//isBsInvisible = false;
					}
					binding.musicProgress.animate().alpha(1f).setDuration(100).start();
                } else {
                    activity.innerBottomSheetBehavior.setDraggable(false);
                    //activity.playerState = activity.UIState.MOVING;
					//isBsInvisible = false;
				}
			}
				
			@Override
			public void onSlide(float slideOffset) {
                if (binding.lyricsContainer.getVisibility() != View.GONE) return;
                activity.currentSlideOffset = slideOffset;
                float clampedOffset = Math.max(0f, slideOffset);
                binding.miniPlayerBottomSheet.setProgress(clampedOffset);
                binding.fragmentsContainer.setTranslationY(-XUtils.convertToPx(activity, 75f)*clampedOffset);
                if (activity.isBlurOn && XUtils.areBlursOrDynamicColorsSupported()) binding.fragmentsContainer.setRenderEffect(RenderEffect.createBlurEffect(25f*clampedOffset, 25f*clampedOffset, Shader.TileMode.CLAMP));
                binding.Scrim.setAlpha(clampedOffset*0.8f);
                if (!activity.isBNVHidden()) binding.bottomNavigation.setTranslationY(binding.bottomNavigation.getHeight()*slideOffset*2.5f);
				if (slideOffset >= 0f) {
					if (slideOffset <= 0.05f) {
						binding.miniPlayerDetailsLayout.setAlpha(1f - slideOffset*20);
						if (activity.isAnimated) {
							activity.isAnimated = false;
                        }
                    } else {
                        if (!activity.isAnimated) {
                            binding.miniPlayerDetailsLayout.animate().alpha(0f).setDuration(80).start();
							activity.isAnimated = true;
						}
					}
					if (slideOffset >= 0.5f) {
                        activity.callback.setEnabled(true);
						activity.isColorAnimated = false;
					    activity.tmpColor = XUtils.interpolateColor(activity.bottomSheetColor, activity.playerSurface, slideOffset*2 - 1f);
						binding.expressiveBottomSheet.setSheetBackgroundColor(activity.tmpColor);
                        Drawable background2 = binding.extendableLayout.getBackground();
						((GradientDrawable) background2).setColor(activity.tmpColor);
						binding.songSeekbar.setEnabled(true);
					} else {
                        if (!activity.isCallbackValid) activity.callback.setEnabled(false);
						if (!activity.isColorAnimated) {
							activity.isColorAnimated = true;
							XUtils.animateColor(activity.tmpColor, activity.bottomSheetColor, animation -> {
								int animatedColor = (int) animation.getAnimatedValue();
								binding.expressiveBottomSheet.setSheetBackgroundColor(animatedColor);
								Drawable background2 = binding.extendableLayout.getBackground();
								((GradientDrawable) background2).setColor(animatedColor);
                            });
                        }
                        binding.songSeekbar.setEnabled(false);
				    }
                    binding.expressiveBottomSheet.getPredictiveBackCallback().setEnabled(slideOffset >= 0f);
				} else {
                    activity.helper.animateTopCorners(XUtils.convertToPx(activity, 18f), 500);
                    XUtils.animateColor(activity.tmpColor, activity.bottomSheetColor, animation -> {
						int animatedColor = (int) animation.getAnimatedValue();
						binding.expressiveBottomSheet.setSheetBackgroundColor(animatedColor);
                    });
                }
			}
		});
        
        activity.innerBottomSheetBehavior = BottomSheetBehavior.from(binding.extendableLayout);
        activity.innerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        activity.innerBottomSheetBehavior.setDraggable(true);
        
        activity.innerBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    binding.expressiveBottomSheet.setDraggable(true);
                    activity.callback.setEnabled(true);
                    activity.callback2.setEnabled(false);
                } else {
                    binding.expressiveBottomSheet.setDraggable(false);
                    activity.callback.setEnabled(false);
                    activity.callback2.setEnabled(true);
                }
            }
            
            @Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float prog = 1f - slideOffset;
                binding.extendableLayout.setTranslationY(activity.statusBarHeight*slideOffset);
                
                Drawable background = binding.extendableLayout.getBackground();
                int color = XUtils.interpolateColor(LiveColors.surface, LiveColors.surfaceContainer, slideOffset);
				((GradientDrawable) background).setColor(color);
                if (slideOffset >= 0f) {
                    if (slideOffset > 0f) {
                        binding.expressiveBottomSheet.setDraggable(false);
                        activity.callback.setEnabled(false);
                        activity.callback2.setEnabled(true);
                    } else {
                        binding.expressiveBottomSheet.setDraggable(true);
                        activity.callback.setEnabled(true);
                        activity.callback2.setEnabled(false);
                    }
                }
            }
        });
	}
    
}

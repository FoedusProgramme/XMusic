package com.xapps.media.xmusic.activity.manager;

import android.animation.ValueAnimator;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import androidx.core.view.ViewKt;
import androidx.transition.TransitionManager;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.widget.*;
import kotlin.Unit;

public class UIManager {

    private RootActivity activity;
    private ActivityRootBinding binding;
    
    public static final int LAYOUT_STATE_FULL = 0;
    public static final int LAYOUT_STATE_EXPOSE_TABS = 1;
    public static final int LAYOUT_STATE_EXPOSE_BNV = 2;
    public static final int LAYOUT_STATE_EXPOSE_PLAYER = 3;
    public static final int LAYOUT_STATE_EXPOSE_PLAYER_ONLY = 4;
    public static final int LAYOUT_STATE_EXPOSE_PLAYER_AND_TABS = 5;
    public static final int LAYOUT_STATE_EXPOSE_TABS_BNV = 6;
    public static final int LAYOUT_STATE_EXPOSE_FULL = 7;
    
    public static final int ANIMATION_DURATION_SHORT = 300;
    public static final int ANIMATION_DURATION_NORMAL = 500;
    
    public int bnvNeededMargin, playerNeededMargin, tabsNeededMargin, playerDockedNeededMargin;
    
    private int layoutState = LAYOUT_STATE_FULL;
    
    private boolean tabsHidden, bnvHidden, playerDocked;
    
    private Interpolator interpolator;
    
    private int bnvHeight;

    public UIManager(RootActivity activity) {
        this.activity = activity;
        this.binding = activity.getBinding();
    }

    public void initUI() {
        setupColors();
        setupShapes();
        setupDimensions();
        setupInitialState();
    }

    private void setupColors() {
        binding.rootCard.setCardBackgroundColor(MaterialColorUtils.colorSurfaceContainer);
        binding.miniPlayer.setSheetBackgroundColor(MaterialColorUtils.colorSurfaceContainer);
        
        binding.bottomNavigation.setBackground(new ColorDrawable(MaterialColorUtils.colorSurface));
    }

    private void setupDimensions() {
        int sideMargins = XUtils.convertToPx(activity, 8f);
        binding.miniPlayer.setPeekHeight(200);
        
        ViewKt.doOnLayout(binding.bottomNavigation, v -> {
            int bottomMargin = XUtils.convertToPx(activity, 16f) + binding.bottomNavigation.getHeight() - XUtils.getNavigationBarHeight(activity);
            binding.miniPlayer.setFloatingMargins(sideMargins, bottomMargin);
            
            playerNeededMargin = binding.miniPlayer.getPeekHeight() + XUtils.convertToPx(activity, 24f) + binding.bottomNavigation.getHeight();
            playerDockedNeededMargin = binding.miniPlayer.getPeekHeight() + XUtils.convertToPx(activity, 40f) + XUtils.getNavigationBarHeight(activity);
            bnvNeededMargin = binding.bottomNavigation.getHeight() + XUtils.convertToPx(activity, 8f);
            
            bnvHeight = binding.bottomNavigation.getHeight();
            
            return Unit.INSTANCE;
        });
        
        ViewKt.doOnLayout(binding.tabLayout, v -> {
            XUtils.increaseMargins(binding.tabLayout, 0, XUtils.getStatusBarHeight(activity), 0, 0);
            
            tabsNeededMargin = binding.tabLayout.getHeight() + XUtils.getStatusBarHeight(activity) + XUtils.convertToPx(activity, 8f);
            
            return Unit.INSTANCE;
        });
    }

    private void setupShapes() {
        int cornerRadius = XUtils.convertToPx(activity, 12f);
        binding.miniPlayer.setFloatingCornerRadii(cornerRadius, cornerRadius, cornerRadius, cornerRadius);
    }

    private void setupInitialState() {
        binding.miniPlayer.setState(ExpressiveSliderLayout.STATE_COLLAPSED);
        
        TypedValue tv = new TypedValue();
        activity.getTheme().resolveAttribute(
            com.google.android.material.R.attr.motionEasingEmphasizedInterpolator,
            tv,
            true
        );
        interpolator = AnimationUtils.loadInterpolator(activity, tv.resourceId);
    }

    public void setLayoutState(int state) {
        if (layoutState == state) return;
        if (state > 7 || state < 0) throw new IllegalArgumentException("Layout state invalid");
        layoutState = state;
        switch (state) {
            case LAYOUT_STATE_EXPOSE_PLAYER:
                hideBnv(false);
                hideTabs(true);
                hidePlayer(false);
                XUtils.animateMarginsTo(binding.rootCard, 0, 0, 0, playerNeededMargin, 500, interpolator);
                dockPlayer(false);
            break;
            
            case LAYOUT_STATE_EXPOSE_BNV:
                hideBnv(false);
                hideTabs(true);
                hidePlayer(true);
                XUtils.animateMarginsTo(binding.rootCard, 0, 0, 0, bnvNeededMargin, 500, interpolator);
            break;
            
            case LAYOUT_STATE_FULL:
                hideBnv(true);
                hideTabs(true);
                hidePlayer(true);
                XUtils.animateMarginsTo(binding.rootCard, 0, 0, 0, 0, 500, interpolator);
            break;
            
            case LAYOUT_STATE_EXPOSE_FULL:
                hideBnv(false);
                hideTabs(false);
                hidePlayer(false);
                XUtils.animateMarginsTo(binding.rootCard, 0, tabsNeededMargin, 0, playerNeededMargin, 500, interpolator);
                dockPlayer(false);
            break;
            
            case LAYOUT_STATE_EXPOSE_TABS_BNV:
                hideBnv(false);
                hideTabs(false);
                hidePlayer(true);
                XUtils.animateMarginsTo(binding.rootCard, 0, tabsNeededMargin, 0, bnvNeededMargin, 500, interpolator);
            break;
            
            case LAYOUT_STATE_EXPOSE_PLAYER_ONLY:
                hideBnv(true);
                hideTabs(true);
                hidePlayer(false);
                XUtils.animateMarginsTo(binding.rootCard, 0, 0, 0, playerDockedNeededMargin, 500, interpolator);
                dockPlayer(true);
            break;
            
            case LAYOUT_STATE_EXPOSE_TABS:
                hideBnv(true);
                hideTabs(true);
                hidePlayer(true);
                XUtils.animateMarginsTo(binding.rootCard, 0, tabsNeededMargin, 0, 0, 500, interpolator);
            break;
        }
    }

    private void hideBnv(boolean hide) {
        binding.bottomNavigation.animate().scaleX(hide? 0.85f : 1f).scaleY(hide? 0.85f : 1f).translationY(hide? 15 : 0).alpha(hide? 0f : 1f).setDuration(ANIMATION_DURATION_NORMAL).withStartAction(() -> {
            if (!hide) binding.bottomNavigation.setVisibility(View.VISIBLE);
        }).withEndAction(() -> {
            if (hide) binding.bottomNavigation.setVisibility(View.GONE);
        }).setInterpolator(interpolator).start();
    }

    private void hideTabs(boolean hide) {
        binding.tabLayout.animate().scaleX(hide? 0.85f : 1f).scaleY(hide? 0.85f : 1f).translationY(hide? -15: 0).alpha(hide? 0f : 1f).setDuration(ANIMATION_DURATION_NORMAL).withStartAction(() -> {
            if (!hide) binding.tabLayout.setVisibility(View.VISIBLE);
        }).withEndAction(() -> {
            if (hide) binding.tabLayout.setVisibility(View.GONE);
        }).setInterpolator(interpolator).start();
    }
    
    private void hidePlayer(boolean hide) {
        /*binding.miniPlayer.animate().alpha(hide? 0f : 1f).translationY(hide? -15 : 0).scaleX(hide? 0.85f : 1f).scaleY(hide? 0.85f : 1f).setDuration(ANIMATION_DURATION_NORMAL).withStartAction(() -> {
            if (!hide) binding.miniPlayer.setVisibility(View.VISIBLE);
        }).withEndAction(() -> {
            if (hide) binding.miniPlayer.setVisibility(View.GONE);
        }).setInterpolator(interpolator).start();*/
        binding.miniPlayer.setState(hide? ExpressiveSliderLayout.STATE_HIDDEN : ExpressiveSliderLayout.STATE_COLLAPSED);
    }

    private void dockPlayer(boolean dock) {
        int bottomMargin = XUtils.convertToPx(activity, 16f) + bnvHeight - XUtils.getNavigationBarHeight(activity);
        
        ValueAnimator animator = ValueAnimator.ofInt(binding.miniPlayer.getFloatingMargin("bottom"), dock? XUtils.getNavigationBarHeight(activity) : XUtils.convertToPx(activity, 8f) + bnvHeight - XUtils.getNavigationBarHeight(activity));
		animator.setDuration(ANIMATION_DURATION_NORMAL);
        animator.setInterpolator(interpolator);
		animator.addUpdateListener(animation -> {
			int margin = (int) animation.getAnimatedValue();
			binding.miniPlayer.setFloatingMargins(XUtils.convertToPx(activity, 8f), margin);
		});
		
		animator.start();
    }
}

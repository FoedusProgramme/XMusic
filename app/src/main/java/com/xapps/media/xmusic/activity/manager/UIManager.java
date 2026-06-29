package com.xapps.media.xmusic.activity.manager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.SeekBar;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewKt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.transition.TransitionManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.callback.CallbackInterface;
import com.xapps.media.xmusic.data.LiveColors;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.databinding.LayoutPlayerCollapsedBinding;
import com.xapps.media.xmusic.fragment.SettingsFragment;
import com.xapps.media.xmusic.fragment.SongsListFragment;
import com.xapps.media.xmusic.utils.ColorPaletteUtils;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.widget.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.Unit;

public class UIManager {

    private RootActivity activity;
    private ActivityRootBinding binding;
    private LayoutPlayerCollapsedBinding smallPlayer;
    
    public static final int LAYOUT_STATE_FULL = 0;
    public static final int LAYOUT_STATE_EXPOSE_TABS = 1;
    public static final int LAYOUT_STATE_EXPOSE_BNV = 2;
    public static final int LAYOUT_STATE_EXPOSE_PLAYER = 3;
    public static final int LAYOUT_STATE_EXPOSE_PLAYER_ONLY = 4;
    public static final int LAYOUT_STATE_EXPOSE_PLAYER_TABS = 5;
    public static final int LAYOUT_STATE_EXPOSE_TABS_BNV = 6;
    public static final int LAYOUT_STATE_EXPOSE_FULL = 7;
    
    public static final int ANIMATION_DURATION_INSTANT = 0;
    public static final int ANIMATION_DURATION_SHORT = 300;
    public static final int ANIMATION_DURATION_NORMAL = 500;
    
    public int bnvNeededMargin, playerNeededMargin, tabsNeededMargin, playerDockedNeededMargin;
    
    private int layoutState = LAYOUT_STATE_FULL;
    
    private volatile long metadataRequestId;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private final ExecutorService metadataExecutor = Executors.newSingleThreadExecutor();
    
    public boolean tabsHidden, bnvHidden, playerDocked, playerHidden;
    
    private boolean isOledTheme;
    
    public int peekHeight;
    
    private Interpolator interpolator;
    
    private ValueAnimator colorAnimator;
    
    private int bnvHeight;
    
    private int playerSurface, bottomSheetColor;
    
    private Map<String, Integer> effectiveOldColors = new HashMap<>();

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
        peekHeight = XUtils.convertToPx(activity, 64f) + binding.collapsedPlayer.musicProgress.getHeight();
        binding.miniPlayer.setPeekHeight(peekHeight);
        
        ViewKt.doOnLayout(binding.bottomNavigation, v -> {
            int bottomMargin = XUtils.convertToPx(activity, 16f) + binding.bottomNavigation.getHeight() - XUtils.getNavigationBarHeight(activity);
            binding.miniPlayer.setFloatingMargins(sideMargins, bottomMargin);
            
            playerNeededMargin = binding.miniPlayer.getPeekHeight() + XUtils.convertToPx(activity, 24f) + binding.bottomNavigation.getHeight();
            playerDockedNeededMargin = binding.miniPlayer.getPeekHeight() + XUtils.convertToPx(activity, 40f) + XUtils.getNavigationBarHeight(activity);
            bnvNeededMargin = binding.bottomNavigation.getHeight() + XUtils.convertToPx(activity, 8f);
            
            bnvHeight = binding.bottomNavigation.getHeight();
            
            ViewKt.doOnLayout(binding.tabLayout, v2 -> {
                
                setLayoutState(LAYOUT_STATE_EXPOSE_TABS_BNV, true);
                return Unit.INSTANCE;
            });
            
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
        binding.collapsedPlayer.cover.setRadius(XUtils.convertToPx(activity, 8f));
    }

    private void setupInitialState() {
        TypedValue tv = new TypedValue();
        activity.getTheme().resolveAttribute(
            com.google.android.material.R.attr.motionEasingEmphasizedInterpolator,
            tv,
            true
        );
        interpolator = AnimationUtils.loadInterpolator(activity, tv.resourceId);
        
        binding.tabLayout.addTab("Songs");
        binding.tabLayout.addTab("Artists");
        binding.tabLayout.addTab("Albums");
        binding.tabLayout.addTab("Genres");
        binding.tabLayout.addTab("Playlists");
        binding.tabLayout.addTab("Browse");
        
        binding.viewpager.setAdapter(new PagerAdapter(activity));
        
        binding.containerRoot.setClipChildren(false);
        binding.miniPlayer.setClipChildren(false);
    }
    
    // [ -------------- Layout State management methods ----------
    
    public void setLayoutState(int state) {
        setLayoutState(state, true);
    }

    public void setLayoutState(int state, boolean animate) {
        if (layoutState == state) return;
        if (state > 7 || state < 0) throw new IllegalArgumentException("Invalid state int");
        
        int duration = animate? ANIMATION_DURATION_NORMAL : 0;
        
        layoutState = state;
        switch (state) {
            case LAYOUT_STATE_EXPOSE_PLAYER:
                hideBnvInternal(false, animate);
                hideTabsInternal(true, animate);
                hidePlayerInternal(false);
                XUtils.animateMarginsTo(binding.rootCard, 0, 0, 0, playerNeededMargin, duration, interpolator);
                dockPlayerInternal(false, animate);
            break;
            
            case LAYOUT_STATE_EXPOSE_BNV:
                hideBnvInternal(false, animate);
                hideTabsInternal(true, animate);
                hidePlayerInternal(true);
                XUtils.animateMarginsTo(binding.rootCard, 0, 0, 0, bnvNeededMargin, duration, interpolator);
            break;
            
            case LAYOUT_STATE_FULL:
                hideBnvInternal(true, animate);
                hideTabsInternal(true, animate);
                hidePlayerInternal(true);
                XUtils.animateMarginsTo(binding.rootCard, 0, 0, 0, 0, duration, interpolator);
            break;
            
            case LAYOUT_STATE_EXPOSE_FULL:
                hideBnvInternal(false, animate);
                hideTabsInternal(false, animate);
                hidePlayerInternal(false);
                XUtils.animateMarginsTo(binding.rootCard, 0, tabsNeededMargin, 0, playerNeededMargin, duration, interpolator);
                dockPlayerInternal(false, animate);
            break;
            
            case LAYOUT_STATE_EXPOSE_TABS_BNV:
                hideBnvInternal(false, animate);
                hideTabsInternal(false, animate);
                hidePlayerInternal(true);
                XUtils.animateMarginsTo(binding.rootCard, 0, tabsNeededMargin, 0, bnvNeededMargin, duration, interpolator);
            break;
            
            case LAYOUT_STATE_EXPOSE_PLAYER_ONLY:
                hideBnvInternal(true, animate);
                hideTabsInternal(true, animate);
                hidePlayerInternal(false);
                XUtils.animateMarginsTo(binding.rootCard, 0, 0, 0, playerDockedNeededMargin, duration, interpolator);
                dockPlayerInternal(true, animate);
            break;
            
            case LAYOUT_STATE_EXPOSE_TABS:
                hideBnvInternal(true, animate);
                hideTabsInternal(false, animate);
                hidePlayerInternal(true);
                XUtils.animateMarginsTo(binding.rootCard, 0, tabsNeededMargin, 0, 0, duration, interpolator);
            break;
            
            case LAYOUT_STATE_EXPOSE_PLAYER_TABS:
                hideBnvInternal(true, animate);
                hideTabsInternal(false, animate);
                hidePlayerInternal(false);
                XUtils.animateMarginsTo(binding.rootCard, 0, tabsNeededMargin, 0, playerDockedNeededMargin, duration, interpolator);
            break;
            
        }
    }

    private void hideBnvInternal(boolean hide, boolean animate) {
        bnvHidden = hide;
        binding.bottomNavigation.animate().scaleX(hide? 0.85f : 1f).scaleY(hide? 0.85f : 1f).translationY(hide? 15 : 0).alpha(hide? 0f : 1f).setDuration(animate? ANIMATION_DURATION_NORMAL : ANIMATION_DURATION_INSTANT).withStartAction(() -> {
            if (!hide) binding.bottomNavigation.setVisibility(View.VISIBLE);
        }).withEndAction(() -> {
            if (hide) binding.bottomNavigation.setVisibility(View.GONE);
        }).setInterpolator(interpolator).start();
    }

    private void hideTabsInternal(boolean hide, boolean animate) {
        tabsHidden = hide;
        binding.tabLayout.animate().scaleX(hide? 0.85f : 1f).scaleY(hide? 0.85f : 1f).translationY(hide? -15: 0).alpha(hide? 0f : 1f).setDuration(animate? ANIMATION_DURATION_NORMAL : ANIMATION_DURATION_INSTANT).withStartAction(() -> {
            if (!hide) binding.tabLayout.setVisibility(View.VISIBLE);
        }).withEndAction(() -> {
            if (hide) binding.tabLayout.setVisibility(View.GONE);
        }).setInterpolator(interpolator).start();
    }
    
    private void hidePlayerInternal(boolean hide) {
        playerHidden = hide;
        binding.miniPlayer.setState(hide? ExpressiveSliderLayout.STATE_HIDDEN : ExpressiveSliderLayout.STATE_COLLAPSED);
    }

    private void dockPlayerInternal(boolean dock, boolean animate) {
        playerDocked = dock;
        int bottomMargin = XUtils.convertToPx(activity, 16f) + bnvHeight - XUtils.getNavigationBarHeight(activity);
        
        ValueAnimator animator = ValueAnimator.ofInt(binding.miniPlayer.getFloatingMargin("bottom"), dock? XUtils.getNavigationBarHeight(activity) : XUtils.convertToPx(activity, 8f) + bnvHeight - XUtils.getNavigationBarHeight(activity));
		animator.setDuration(animate? ANIMATION_DURATION_NORMAL : ANIMATION_DURATION_INSTANT);
        animator.setInterpolator(interpolator);
		animator.addUpdateListener(animation -> {
			int margin = (int) animation.getAnimatedValue();
			binding.miniPlayer.setFloatingMargins(XUtils.convertToPx(activity, 8f), margin);
		});
		
		animator.start();
    }

    public void onPlayerHidden() {
        if (bnvHidden && tabsHidden) {
            setLayoutState(LAYOUT_STATE_FULL);
        } else if (!bnvHidden && tabsHidden) {
            setLayoutState(LAYOUT_STATE_EXPOSE_BNV);
        } else if (bnvHidden && !tabsHidden) {
            setLayoutState(LAYOUT_STATE_EXPOSE_TABS);
        } else if (!bnvHidden && !tabsHidden) {
            setLayoutState(LAYOUT_STATE_EXPOSE_TABS_BNV);
        } else {
            throw new IllegalStateException("onPlayerHidden was without handling layout state : " + String.valueOf(binding.miniPlayer.getState()));
        }
    }  
    
    public void hideComponents(boolean hidePlayer, boolean hideBnv, boolean hideTabs) {
        int state =
        (!hidePlayer ? 4 : 0) |
        (!hideBnv ? 2 : 0) |
        (!hideTabs ? 1 : 0);

        setLayoutState(state);
    }

    // --------------- Fragments Viewpager Adapter ----------------- ]

    public class PagerAdapter extends FragmentStateAdapter {

        public PagerAdapter(FragmentActivity activity) {
            super(activity);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return new SongsListFragment();
            return new SettingsFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    // PLAYER ANIMATION AND UPDATES LOGIC -------------

    public void updateColors() {
        if (ColorPaletteUtils.lightColors == null && ColorPaletteUtils.darkColors == null) return;
        
        Map<String, Integer> colors = XUtils.isDarkMode(activity) ? ColorPaletteUtils.darkColors : ColorPaletteUtils.lightColors;
        Map<String, Integer> oldColors = XUtils.isDarkMode(activity) ? ColorPaletteUtils.oldDarkColors : ColorPaletteUtils.oldLightColors;
        
        effectiveOldColors = new HashMap<>(oldColors);
        
        boolean hasLive = LiveColors.primary != 0;
        int onTertiary = colors.get("onTertiary");
        int tertiary = colors.get("tertiary");
        int oldOnTertiary = hasLive ? LiveColors.onTertiary : effectiveOldColors.get("onTertiary");
        int oldTertiary   = hasLive ? LiveColors.tertiary   : effectiveOldColors.get("tertiary");
        int surface = isOledTheme ? 0xff000000 : colors.get("surface");
        int oldSurface = isOledTheme ? 0xff000000 : (hasLive ? LiveColors.surface : effectiveOldColors.get("surface"));
        int surfaceContainer = isOledTheme ? 0xff050505 : colors.get("surfaceContainer");
        int oldSurfaceContainer = isOledTheme ? 0xff050505 : (hasLive ? LiveColors.surfaceContainer : effectiveOldColors.get("surfaceContainer"));
        int outline = colors.get("outline");
        int oldOutline = hasLive ? LiveColors.outline : effectiveOldColors.get("outline");
        int primary = colors.get("primary");
        int oldPrimary = hasLive ? LiveColors.primary : effectiveOldColors.get("primary");
        int onPrimary = colors.get("onPrimary");
        int oldOnPrimary = hasLive ? LiveColors.onPrimary : effectiveOldColors.get("onPrimary");
        int onSurfaceContainer = isOledTheme? colors.get("onSurface") : colors.get("onSurfaceContainer");
        int oldOnSurfaceContainer = isOledTheme ? (hasLive ? LiveColors.onSurface : effectiveOldColors.get("onSurface")) : (hasLive ? LiveColors.onSurfaceContainer : effectiveOldColors.get("onSurfaceContainer"));
        int onSurface = colors.get("onSurface");
        int oldOnSurface = hasLive ? LiveColors.onSurface : effectiveOldColors.get("onSurface");
        
        binding.expandedPlayer.mesh.setColors(surface, onPrimary, onTertiary);
        
        Drawable nextBg = binding.expandedPlayer.nextButton.getBackground();
        Drawable favBg  = binding.expandedPlayer.favoriteButton.getBackground();
        Drawable saveBg = binding.expandedPlayer.saveButton.getBackground();
        Drawable prevBg = binding.expandedPlayer.previousButton.getBackground();
        
        GradientDrawable d3 = (GradientDrawable) binding.expandedPlayer.songInfoLayout.getBackground();
        
        XSeekbar seekbar = binding.expandedPlayer.songSeekbar;
        
        
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(500);
        va.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            int iop = XUtils.interpolateColor(oldOnPrimary, onPrimary, f);
            int ip = XUtils.interpolateColor(oldPrimary, primary, f);
            int iot = XUtils.interpolateColor(oldOnTertiary, onTertiary, f);
            int it = XUtils.interpolateColor(oldTertiary, tertiary, f);
            int is = XUtils.interpolateColor(oldSurface, surface, f);
            int isc = XUtils.interpolateColor(oldSurfaceContainer, surfaceContainer, f);
            int io = XUtils.interpolateColor(oldOutline, outline, f);
            int iosc = XUtils.interpolateColor(oldOnSurfaceContainer, onSurfaceContainer, f);
            int ios = XUtils.interpolateColor(oldOnSurface, onSurface, f);
            
            LiveColors.primary = ip;
            LiveColors.onPrimary = iop;
            LiveColors.tertiary = it;
            LiveColors.onTertiary = iot;
            LiveColors.surface = is;
            LiveColors.surfaceContainer = isc;
            LiveColors.outline = io;
            LiveColors.onSurface = ios;
            LiveColors.onSurfaceContainer = iosc;
            
            binding.expandedPlayer.toggleView.setShapeColor(iop);
            binding.expandedPlayer.toggleView.setIconColor(ip);
            binding.expandedPlayer.xlyricsView.setLyricColor(ios, io);
			binding.expandedPlayer.placeholderLyricsText.setTextColor(ios);
			
            binding.expandedPlayer.nextButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            binding.expandedPlayer.favoriteButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            binding.expandedPlayer.saveButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            binding.expandedPlayer.previousButton.setColorFilter(it, PorterDuff.Mode.SRC_IN);
            
            nextBg.setColorFilter(new PorterDuffColorFilter(iot, PorterDuff.Mode.SRC_IN));
            favBg.setColorFilter(new PorterDuffColorFilter(iot, PorterDuff.Mode.SRC_IN));
            saveBg.setColorFilter(new PorterDuffColorFilter(iot, PorterDuff.Mode.SRC_IN));
            prevBg.setColorFilter(new PorterDuffColorFilter(iot, PorterDuff.Mode.SRC_IN));
        
            playerSurface = is;
            
            binding.miniPlayer.setSheetBackgroundColor(playerSurface);
            binding.expandedPlayer.lyricsContainer.setBackgroundColor(playerSurface);
            
            d3.setColor(isc);
            
            seekbar.setColor(ip); 
       
            binding.collapsedPlayer.action.setIconTint(ColorStateList.valueOf(iop));
            binding.collapsedPlayer.action.setBackgroundColor(ip);
            binding.collapsedPlayer.action.setRippleColor(ColorStateList.valueOf(ColorUtils.setAlphaComponent(io, 100)));
            
            binding.expandedPlayer.artistBigTitle.setTextColor(iosc);
            binding.expandedPlayer.songBigTitle.setTextColor(ios);
            
            binding.collapsedPlayer.title.setTextColor(ios);
            binding.collapsedPlayer.subtitle.setTextColor(io);
            
            binding.expandedPlayer.totalDurationText.setTextColor(iosc);
            binding.expandedPlayer.songInfoText.setTextColor(iosc);
        });
        va.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!canceled) {
                    effectiveOldColors = new HashMap<>(colors);
                }
            }
        });
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }

        colorAnimator = va;
        va.start();
    
    }

    public void updateContent(boolean isResuming) {
        if (activity.getController() == null) return;
        int position = activity.getController().getCurrentMediaItemIndex();
        if (position >= 0 && RuntimeData.songs.size() > 0 && position < RuntimeData.songs.size()) syncPlayerUI(position, isResuming);
    }

    public void syncPlayerUI(int position, boolean isResuming) {
        updateMaxValue(position, isResuming);
        updateCoverPager(position);

        if (!isResuming) {
            binding.expandedPlayer.artistBigTitle.animate().alpha(0f).translationX(-20f).setDuration(100).start();
            binding.expandedPlayer.songBigTitle.animate().alpha(0f).translationX(-20f).setDuration(100).start();
            binding.expandedPlayer.totalDurationText.animate().alpha(0f).translationX(-20f).setDuration(100).start();
            binding.expandedPlayer.currentDurationText.animate().alpha(0f).translationX(-20f).setDuration(100).start();

            handler = new Handler(Looper.getMainLooper());

            handler.postDelayed(() -> {
                updateTexts(position, isResuming);
                updateSongInfoLayout(position);

                binding.expandedPlayer.totalDurationText.setTranslationX(20f);
                binding.expandedPlayer.currentDurationText.setTranslationX(20f);
                binding.expandedPlayer.songBigTitle.setTranslationX(20f);
                binding.expandedPlayer.artistBigTitle.setTranslationX(20f);
            }, 110);

            handler.postDelayed(() -> {
                binding.expandedPlayer.artistBigTitle.animate().alpha(1f).translationX(0f).setDuration(120).start();
                binding.expandedPlayer.songBigTitle.animate().alpha(1f).translationX(0f).setDuration(120).start();
                binding.expandedPlayer.currentDurationText.animate().alpha(1f).translationX(0f).setDuration(120).start();
                binding.expandedPlayer.totalDurationText.animate().alpha(1f).translationX(0f).setDuration(120).start();
            }, 120);

        } else {
            updateTexts(position, isResuming);
            updateSongInfoLayout(position);
        }
    }

    public void updateMaxValue(int pos, boolean isRestoring) {
        if (RuntimeData.songs.size() > 0 && activity.getController() != null) {
            int p = activity.getController().getCurrentMediaItemIndex();
			if (pos == -1) return;
            int max = (int) RuntimeData.songs.get(pos == -1? p : pos).duration;
            binding.expandedPlayer.songSeekbar.setMax(max);
            binding.collapsedPlayer.musicProgress.setMax(max);
        } else if (isRestoring) {
            int p = CallbackInterface.service().getCurrentPosition();
			if (p == -1) return;
            int max = (int) RuntimeData.songs.get(pos == -1? p : pos).duration;
            binding.expandedPlayer.songSeekbar.setMax(max);
            binding.collapsedPlayer.musicProgress.setMax(max);
        }
    }

    public void updateTexts(int pos, boolean isRestoring) {
        if (RuntimeData.songs.size() > 0 && activity.getController() != null) {
            if (pos == -1) return;
            binding.expandedPlayer.totalDurationText.setText(RuntimeData.songs.get(pos).getFormattedDuration());
            binding.expandedPlayer.artistBigTitle.setText(RuntimeData.songs.get(pos).artist);
            binding.expandedPlayer.songBigTitle.setText(RuntimeData.songs.get(pos).title);
            binding.collapsedPlayer.title.setText(RuntimeData.songs.get(pos).title);
            binding.collapsedPlayer.subtitle.setText(RuntimeData.songs.get(pos).artist);
    
        } else if (isRestoring && CallbackInterface.service() != null && CallbackInterface.service().isAnythingPlaying()) {
            int p = CallbackInterface.service().getCurrentPosition();
            if (p == -1) return;
    
            binding.expandedPlayer.totalDurationText.setText(RuntimeData.songs.get(p).getFormattedDuration());
            binding.expandedPlayer.artistBigTitle.setText(RuntimeData.songs.get(p).artist);
            binding.expandedPlayer.songBigTitle.setText(RuntimeData.songs.get(p).title);
            binding.collapsedPlayer.title.setText(RuntimeData.songs.get(p).title);
            binding.collapsedPlayer.subtitle.setText(RuntimeData.songs.get(p).artist);
        }
    }

    private void updateSongInfoLayout(int pos) {
        if (RuntimeData.songs.isEmpty()) return;
        
        int index = -1;

        if (pos == -1) {
            if (CallbackInterface.service() != null) {
                index = CallbackInterface.service().getCurrentPosition();
                if (index == -1) return;
            } else {
                return;
            }
        }

        final String path;

        try {
            path = RuntimeData.songs.get(index).path;
        } catch (IndexOutOfBoundsException e) {
            return;
        }

        final long requestId = ++metadataRequestId;

        binding.expandedPlayer.songInfoText.animate()
            .alpha(0f)
            .setDuration(100)
            .start();

        metadataExecutor.execute(() -> {

            String mime = "Unknown";
            int kbps = -1;
            String sampleRate = "Unknown";

            try {
                mime = XUtils.getAudioCodec(
                    activity,
                    Uri.fromFile(new File(path))
                );

                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(path);

                String br = mmr.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_BITRATE
                );

                String sr = mmr.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_SAMPLERATE
                );

                mmr.release();

                if (br != null) {
                    kbps = Math.abs(Integer.parseInt(br) / 1000);
                }

                if (sr != null) {
                int hz = Integer.parseInt(sr);
                    sampleRate = hz >= 1000
                        ? (hz / 1000f) + " kHz"
                        : hz + " Hz";
                }

            } catch (Exception ignored) { }

            final String finalMime = mime;
            final int finalKbps = kbps;
            final String finalSampleRate = sampleRate;

            binding.expandedPlayer.songInfoText.post(() -> {

                if (requestId != metadataRequestId) {
                    return;
                }

                String text = finalKbps > 0
                    ? finalMime + " • " + finalKbps + " kbps • " + finalSampleRate
                    : finalMime + " • " + finalSampleRate;

                binding.expandedPlayer.songInfoText.setText(text);
                binding.expandedPlayer.songInfoText.setAlpha(0f);
                binding.expandedPlayer.songInfoText.animate()
                    .alpha(1f)
                    .setDuration(120)
                    .start();
            });
        });
    }

    private void updateCoverPager(int index) {
        if (RuntimeData.songs.isEmpty()) return;
		if (activity.isDestroyed() || activity.isFinishing()) return;
        
        Uri cover = RuntimeData.songs.get(index).getArtworkUri();
        binding.expandedPlayer.coversPager.load(cover);
        binding.collapsedPlayer.cover.load(cover);
    }
}

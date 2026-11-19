package com.xapps.media.xmusic.fragment.base;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.BackEventCompat;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.callback.CallbackInterface;
import com.xapps.media.xmusic.callback.FragmentCallback;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.ui.widget.ExpressiveBottomSheet;

public class SubFragment extends BaseFragment implements FragmentCallback {
	
    private RootActivity ra;
	private int bnvHeight, height;
	private boolean valid, animatePlayer;
	private Fragment f = this;
	private FragmentManager.OnBackStackChangedListener backStackListener;
		
    @Override
    public void onCreate(Bundle b) {
		super.onCreate(b);
        CallbackInterface.setSubFragmentCallback(this);
		ra = (RootActivity) getActivity();
        if (ra == null) {
            return;
        } else {
            if (CallbackInterface.activity() != null) CallbackInterface.activity().passData();
        }
	}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (backStackListener != null) {
            getParentFragmentManager().removeOnBackStackChangedListener(backStackListener);
            backStackListener = null;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        CallbackInterface.clearSubFragmentCallback(this);
        if (backStackListener != null) {
            getParentFragmentManager().removeOnBackStackChangedListener(backStackListener);
            backStackListener = null;
        }
    }
    
    @Override
    public void onBindingReady(ActivityRootBinding binding) {
        if (ra.getUIManager() != null) {
            ra.getUIManager().hideBnv(true);
            ra.getUIManager().saveState();
        }
        
        boolean b1 = ra.getBinding().miniPlayer.getState() == ExpressiveBottomSheet.STATE_COLLAPSED;
        XUtils.animateMarginsTo(ra.getBinding().settingsCard, 0, 0, 0, b1? ra.getUIManager().playerDockedNeededMargin : 0, 200, null);

        if (backStackListener != null) {
            getParentFragmentManager().removeOnBackStackChangedListener(backStackListener);
        }
        backStackListener = new FragmentManager.OnBackStackChangedListener() {
			@Override
			public void onBackStackChanged() {
            }
			
            @Override
            public void onBackStackChangeCommitted(@NonNull Fragment fragment, boolean b) {
                if (b && (fragment == SubFragment.this || isRemoving())) {
                    if (ra != null && ra.getUIManager() != null && ra.getUIManager().bnvHidden) {
                        ra.getUIManager().hideComponents(ra.getUIManager().playerHidden, false, ra.getUIManager().tabsHidden, "SubFragment");
                        ra.getUIManager().saveState();
                    }
                }
            }
			
			@Override
			public void onBackStackChangeCancelled() {
				if (!valid || ra == null) return;
				ra.getBinding().bottomNavigation.animate().translationY(-15).alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(100).start();
                int dockedBottom = XUtils.getNavigationBarHeight(getActivity());
				android.animation.ValueAnimator marginAnim = android.animation.ValueAnimator.ofInt(ra.getBinding().miniPlayer.getFloatingMargin("bottom"), dockedBottom);
                marginAnim.addUpdateListener(animation -> {
                    ra.getBinding().miniPlayer.setFloatingMargins(XUtils.convertToPx(getActivity(), 8f), (int) animation.getAnimatedValue());
                    XUtils.animateMarginsTo(ra.getBinding().settingsCard, 0, 0, 0, animatePlayer? ra.getUIManager().playerDockedNeededMargin : 0, 0, null);
                });
                marginAnim.setDuration(100);
                marginAnim.start();
            }
			
			@Override
			public void onBackStackChangeProgressed(BackEventCompat backEventCompat) {
				if (!valid || ra == null) return;
				ra.getBinding().bottomNavigation.setTranslationY(-15*(1f-backEventCompat.getProgress()));
                if (animatePlayer) {
                    int dockedBottom = XUtils.getNavigationBarHeight(getActivity());
                    int floatingBottom = XUtils.convertToPx(getActivity(), 16f) + bnvHeight;
                    int currentBottom = Math.round(dockedBottom + (floatingBottom - dockedBottom) * backEventCompat.getProgress());
                    ra.getBinding().miniPlayer.setFloatingMargins(XUtils.convertToPx(getActivity(), 8f), currentBottom);
                }
				ra.getBinding().bottomNavigation.setAlpha(1f*backEventCompat.getProgress());
                ra.getBinding().bottomNavigation.setScaleX(0.85f + 0.15f * backEventCompat.getProgress());
                ra.getBinding().bottomNavigation.setScaleY(0.85f + 0.15f * backEventCompat.getProgress());
                XUtils.setMargins(ra.getBinding().settingsCard, 0, 0, 0, Math.round(((animatePlayer? ra.getUIManager().playerDockedNeededMargin + (ra.getUIManager().playerNeededMargin - ra.getUIManager().playerDockedNeededMargin) * backEventCompat.getProgress(): ra.getUIManager().bnvNeededMargin * backEventCompat.getProgress()))));
            }

            @Override
			public void onBackStackChangeStarted(Fragment fragment, boolean z) {
                if (ra == null) return;
                valid = z && (fragment == SubFragment.this || (f.isAdded() && !f.isRemoving()));
                if (!valid) return;
                ra.getBinding().bottomNavigation.animate().cancel();
                ra.getBinding().bottomNavigation.setVisibility(View.VISIBLE);
                animatePlayer = ra.getBinding().miniPlayer.getState() == ExpressiveBottomSheet.STATE_COLLAPSED;
                bnvHeight = ra.getBinding().bottomNavigation.getHeight();
                height = animatePlayer? ra.getUIManager().playerNeededMargin : ra.getUIManager().bnvNeededMargin;
            }
			
        };
        getParentFragmentManager().addOnBackStackChangedListener(backStackListener);
    }

}

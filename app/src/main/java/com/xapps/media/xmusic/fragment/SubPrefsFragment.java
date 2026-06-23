package com.xapps.media.xmusic.fragment;
import android.os.Bundle;
import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.utils.XUtils;

public class SubPrefsFragment extends BasePrefsFragment {
	
    private MainActivity ma;
	private int bnvHeight;
	private boolean valid;
	private Fragment f = this;
		
    @Override
    public void onCreate(Bundle b) {
		super.onCreate(b);
		ma = (MainActivity) getActivity();
	    ma.HideBNV(true);
		getParentFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
			@Override
			public void onBackStackChanged() {
            }
			
            @Override
            public void onBackStackChangeCommitted(Fragment fragment, boolean b) {
                if (isRemoving() && valid) { 
                    ma.HideBNV(false);
                }
            }
			
			@Override
			public void onBackStackChangeCancelled() {
				if (!valid) return;
				ma.getBinding().bottomNavigation.animate().translationY(bnvHeight).alpha(0.5f).setDuration(100).start();
				android.animation.ValueAnimator marginAnim = android.animation.ValueAnimator.ofInt(ma.getBinding().expressiveBottomSheet.getFloatingMargin("bottom"), /*XUtils.getNavigationBarHeight(getActivity()) + */XUtils.convertToPx(getActivity(), 4f));
                marginAnim.addUpdateListener(animation -> ma.getBinding().expressiveBottomSheet.setFloatingMargins(XUtils.convertToPx(getActivity(), 12f), (int) animation.getAnimatedValue()));
                marginAnim.setDuration(100);
                marginAnim.start();
            }
			
			@Override
			public void onBackStackChangeProgressed(BackEventCompat backEventCompat) {
				if (!valid) return;
				ma.getBinding().bottomNavigation.setTranslationY(bnvHeight*(1f-backEventCompat.getProgress()));
                ma.getBinding().expressiveBottomSheet.setFloatingMargins(XUtils.convertToPx(getActivity(), 12f), Math.round(XUtils.convertToPx(getActivity(), 4f) + (bnvHeight-XUtils.getNavigationBarHeight(getActivity()))*(backEventCompat.getProgress())));
				ma.getBinding().bottomNavigation.setAlpha(0.5f+(0.5f*backEventCompat.getProgress()));
            }

            @Override
			public void onBackStackChangeStarted(Fragment fragment, boolean z) {
				bnvHeight = ma.getBinding().bottomNavigation.getHeight();
				valid = z && (f.isAdded() && !(f.isRemoving()));
            }
			
        });
	}

}

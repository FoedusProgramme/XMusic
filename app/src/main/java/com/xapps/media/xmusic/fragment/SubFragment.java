package com.xapps.media.xmusic.fragment;
import android.os.Bundle;
import androidx.activity.BackEventCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.utils.XUtils;

public class SubFragment extends BaseFragment {
	
    private RootActivity ra;
	private int bnvHeight;
	private boolean valid;
	private Fragment f = this;
		
    @Override
    public void onCreate(Bundle b) {
		super.onCreate(b);
		ra = (RootActivity) getActivity();
	    //ra.HideBNV(true); TODO : FIX
		getParentFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
			@Override
			public void onBackStackChanged() {
            }
			
            @Override
            public void onBackStackChangeCommitted(Fragment fragment, boolean b) {
                if (isRemoving() && valid) { 
                    //ra.HideBNV(false);
                }
            }
			
			@Override
			public void onBackStackChangeCancelled() {
				if (!valid) return;
				ra.getBinding().bottomNavigation.animate().translationY(bnvHeight).alpha(0.5f).setDuration(100).start();
				android.animation.ValueAnimator marginAnim = android.animation.ValueAnimator.ofInt(ra.getBinding().miniPlayer.getFloatingMargin("bottom"), XUtils.convertToPx(getActivity(), 4f));
                marginAnim.addUpdateListener(animation -> ra.getBinding().miniPlayer.setFloatingMargins(XUtils.convertToPx(getActivity(), 12f), (int) animation.getAnimatedValue()));
                marginAnim.setDuration(100);
                marginAnim.start();
            }
			
			@Override
			public void onBackStackChangeProgressed(BackEventCompat backEventCompat) {
				if (!valid) return;
				ra.getBinding().bottomNavigation.setTranslationY(bnvHeight*(1f-backEventCompat.getProgress()));
                ra.getBinding().miniPlayer.setFloatingMargins(XUtils.convertToPx(getActivity(), 12f), Math.round(XUtils.convertToPx(getActivity(), 4f) + (bnvHeight-XUtils.getNavigationBarHeight(getActivity()))*(backEventCompat.getProgress())));
				ra.getBinding().miniPlayer.setAlpha(0.5f+(0.5f*backEventCompat.getProgress()));
            }

            @Override
			public void onBackStackChangeStarted(Fragment fragment, boolean z) {
				bnvHeight = ra.getBinding().bottomNavigation.getHeight();
				valid = z && (f.isAdded() && !(f.isRemoving()));
            }
			
        });
	}

}

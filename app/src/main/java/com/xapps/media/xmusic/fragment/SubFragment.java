package com.xapps.media.xmusic.fragment;
import android.os.Bundle;
import androidx.activity.BackEventCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.models.BottomSheetBehavior;

public class SubFragment extends BaseFragment {
	
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
				ma.getBinding().miniPlayerBottomSheet.animate().translationY(ma.bnvHeight).setDuration(100).start();
            }
			
			@Override
			public void onBackStackChangeProgressed(BackEventCompat backEventCompat) {
				if (!valid) return;
				ma.getBinding().bottomNavigation.setTranslationY(bnvHeight*(1f-backEventCompat.getProgress()));
				ma.getBinding().miniPlayerBottomSheet.setTranslationY(ma.bnvHeight*(1f-backEventCompat.getProgress()));
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

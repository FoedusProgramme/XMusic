package com.xapps.media.xmusic.fragment;

import android.os.Bundle;
import android.view.Gravity;
import androidx.fragment.app.Fragment;
import androidx.transition.Fade;
import androidx.transition.Slide;
import com.google.android.material.transition.MaterialFade;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;

public class BaseFragment extends Fragment {
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(null);
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }
}

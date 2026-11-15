package com.xapps.media.xmusic.fragment.base;
import com.xapps.media.xmusic.fragment.base.*;
import com.xapps.media.xmusic.fragment.main.*;
import com.xapps.media.xmusic.fragment.settings.*;
import com.xapps.media.xmusic.fragment.info.*;
import com.xapps.media.xmusic.ui.widget.*;
import com.xapps.media.xmusic.ui.lyrics.*;
import com.xapps.media.xmusic.ui.scroll.*;
import com.xapps.media.xmusic.ui.behavior.*;
import com.xapps.media.xmusic.lyrics.*;
import com.xapps.media.xmusic.callback.*;
import com.xapps.media.xmusic.models.*;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.transition.Fade;
import androidx.transition.Slide;
import com.google.android.material.transition.MaterialFade;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.xapps.media.xmusic.R;

public class BaseFragment extends Fragment {
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(null);
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    public void freeze(boolean b) {
        if (getView() == null) return;
		getView().findViewById(R.id.blocking_overlay).setClickable(b);
		getView().findViewById(R.id.blocking_overlay).setFocusable(b);
	}
}

package com.xapps.media.xmusic.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.activity.manager.UIManager;
import com.xapps.media.xmusic.callback.CallbackInterface;
import com.xapps.media.xmusic.callback.FragmentCallback;
import com.xapps.media.xmusic.databinding.FragmentSettingsBinding;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.widget.RichTooltip;

public class SettingsFragment extends BaseFragment implements FragmentCallback {

    private FragmentSettingsBinding binding;

    @NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = FragmentSettingsBinding.inflate(inflater, container, false);
        CallbackInterface.setSgFragCallback(this);
		setupUI();
        setupListeners();
		return binding.getRoot();
	}

    private void setupUI() {
        binding.collapsingtoolbar.setExpandedTitleTextAppearance(R.style.ExpandedAppBarTextStyle);
        TextView t = (TextView) binding.toolbar.getChildAt(0);
        assert getActivity() != null;
        t.setTypeface(ResourcesCompat.getFont(getActivity(), R.font.google_sans_flex));
        t.setFontVariationSettings("'ROND' 100, 'wght' 500");
    }

    private void setupListeners() {
        binding.firstCategory.setOnClickListener(v -> {
            assert getActivity() != null;
            getActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_frag, new AppearanceFragment())
            .addToBackStack("root")
            .commit();
        });

        binding.secondCategory.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_frag, new NowPlayingEditFragment())
            .addToBackStack("root")
            .commit();
        });

		binding.thirdCategory.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_frag, new ExperimentsFragment())
            .addToBackStack("root")
            .commit();
        });

        binding.infoCategory.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_frag, new AboutFragment())
            .addToBackStack("root")
            .commit();
        });
    }

    @Override
    public void freeze(boolean b) {
        binding.blockingOverlay.setClickable(b);
    }

}
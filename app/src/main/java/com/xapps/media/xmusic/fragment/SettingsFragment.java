package com.xapps.media.xmusic.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.databinding.FragmentSettingsBinding;
import com.xapps.media.xmusic.R;

public class SettingsFragment extends BaseFragment {
    
    private FragmentSettingsBinding binding;
    private RootActivity activity;
        
    @NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = FragmentSettingsBinding.inflate(inflater, container, false);
        activity = (RootActivity) getActivity();
		setupUI();
        setupListeners();
		return binding.getRoot();
	}

    private void setupUI() {
        binding.collapsingtoolbar.setExpandedTitleTextAppearance(R.style.ExpandedAppBarTextStyle);
        TextView t = (TextView) binding.toolbar.getChildAt(0);
        t.setTypeface(ResourcesCompat.getFont(getActivity(), R.font.google_sans_flex));
        t.setFontVariationSettings("'ROND' 100, 'wght' 500");
    }
    
    private void setupListeners() {
        binding.firstCategory.setOnClickListener(v -> {
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

}
package com.xapps.media.xmusic.fragment;

import android.graphics.Color;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.transition.Transition;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.transition.MaterialContainerTransform;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.databinding.ActivityMainBinding;
import com.xapps.media.xmusic.databinding.FragmentLibDetailsBinding;
import com.xapps.media.xmusic.utils.XUtils;

public class LibDetailsFragment extends BaseFragment {

    private ActivityMainBinding activityBinding;
    private FragmentLibDetailsBinding binding;
    private MainActivity activity;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLibDetailsBinding.inflate(inflater, container, false);
        activity = (MainActivity) getActivity();
        init(savedInstanceState);
        return binding.getRoot();
    }

    private void init(Bundle data) {
        String name = data.getString("lib_name");
		String license = data.getString("license_text");
    }

    public static LibDetailsFragment newInstance(String name, String text) {
        Bundle args = new Bundle();
        args.putString("lib_name", name);
        args.putString("license_text", text);
        LibDetailsFragment fragment = new LibDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }
}

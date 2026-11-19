package com.xapps.media.xmusic.fragment.info;

import com.xapps.media.xmusic.fragment.base.*;
import com.xapps.media.xmusic.fragment.main.*;
import com.xapps.media.xmusic.fragment.settings.*;
import com.xapps.media.xmusic.ui.widget.*;
import com.xapps.media.xmusic.ui.lyrics.*;
import com.xapps.media.xmusic.ui.scroll.*;
import com.xapps.media.xmusic.ui.behavior.*;
import com.xapps.media.xmusic.lyrics.*;
import com.xapps.media.xmusic.callback.*;
import com.xapps.media.xmusic.models.*;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewKt;
import androidx.fragment.app.Fragment;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.databinding.FragmentLibDetailsBinding;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import kotlin.Unit;

public class LibDetailsFragment extends Fragment {

    private FragmentLibDetailsBinding binding;
    private RootActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLibDetailsBinding.inflate(inflater, container, false);
        activity = (RootActivity) getActivity();
        init();
        return binding.getRoot();
    }

    private void init() {
		Bundle bundle = getArguments();
        String name = bundle.getString("lib_name");
		String license = bundle.getString("license_text");
		binding.toolbar.setTitle(name);
		binding.licenseText.setText(license);
		binding.licenseText.setTypeface(Typeface.MONOSPACE);
		ViewKt.doOnLayout(activity.getBinding().bottomNavigation, v -> {
            binding.licenseText.setPadding(binding.licenseText.getPaddingRight(),  binding.licenseText.getPaddingTop(), binding.licenseText.getPaddingLeft(), binding.licenseText.getPaddingBottom() + activity.getBinding().bottomNavigation.getHeight()*2);
            return Unit.INSTANCE;
        });
		MenuItem item = binding.toolbar.getMenu().add("Website");
        Drawable icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_earth);

        if (icon != null) {
            icon = DrawableCompat.wrap(icon.mutate());
            DrawableCompat.setTint(icon, MaterialColorUtils.colorOnSurface);
            item.setIcon(icon);
        }
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		binding.toolbar.setOnMenuItemClickListener(itm -> {
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(bundle.getString("website", "https://google.com")));
			startActivity(i);
			return true;
		});
    }

    public static LibDetailsFragment newInstance(String name, String text, String w) {
        Bundle args = new Bundle();
        args.putString("lib_name", name);
        args.putString("license_text", text);
		args.putString("website", w);
        LibDetailsFragment fragment = new LibDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }
}

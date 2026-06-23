package com.xapps.media.xmusic.fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewKt;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.databinding.FragmentTestBinding;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.widget.ExpressiveSliderLayout;
import kotlin.Unit;

public class TestFragment extends BaseFragment {
    private MainActivity activity;
    private FragmentTestBinding binding;

    @Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = FragmentTestBinding.inflate(inflater, container, false);
        activity = (MainActivity) getActivity();
        init();
        return binding.getRoot();
	}

    private void init() {
        binding.test.setFloatingCornerRadii(50f, 50f, 16f, 16f);
        binding.test.setSheetBackgroundColor(MaterialColorUtils.colorSurfaceContainer);
        binding.test.setFloatingMargins(40, 400);
        ViewKt.doOnLayout(binding.coversPager, v -> {
            binding.test.setPeekHeight(binding.coversPager.getHeight() + binding.musicProgress.getHeight() + XUtils.convertToPx(getActivity(), 16f));
            return Unit.INSTANCE;
        });
        binding.test.setupPredictiveBack(activity);
        binding.test.addSliderCallback(new ExpressiveSliderLayout.SliderCallback() {
            @Override
            public void onStateChanged(int state) {
                binding.test.getPredictiveBackCallback().setEnabled(state == ExpressiveSliderLayout.STATE_EXPANDED);
            }
            
            @Override
            public void onSlide(float offset) {
                binding.miniPlayerBottomSheet.setProgress(Math.max(0f, Math.min(1f, offset)));
            }
        });
        binding.test.setState(ExpressiveSliderLayout.STATE_COLLAPSED);
    }
}

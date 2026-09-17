package com.xapps.media.xmusic.fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.xapps.media.xmusic.databinding.FragmentContributionsBinding;
import com.xapps.media.xmusic.utils.XUtils;

public class ContributionsFragment extends BaseFragment {
    private FragmentContributionsBinding binding;

    private final String TELEGRAM_LINk = "https://t.me/grimmthejow";
    private final String GITHUB_LINk = "https://github.com/grimmthejow";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContributionsBinding.inflate(inflater, container, false);
        init();
        return binding.getRoot();
    }

    private void init() {
        binding.container.setPadding(0, 0, 0, XUtils.getNavigationBarHeight(requireContext())*2);

        Glide.with(requireContext()).load("https://github.com/grimmthejow.png").into(binding.bigPic);
        Glide.with(requireContext()).load("https://github.com/syntaxspin.png").into(binding.firstIcon);
        Glide.with(requireContext()).load("https://github.com/trindadedev13.png").into(binding.secondIcon);
        Glide.with(requireContext()).load("https://github.com/realekansh.png").into(binding.thirdIcon);
        Glide.with(requireContext()).load("https://github.com/masterzack69.png").into(binding.fourthIcon);
        Glide.with(requireContext()).load("https://github.com/nift4.png").into(binding.fifthIcon);
        Glide.with(requireContext()).load("https://github.com/piyush7890.png").into(binding.sixthIcon);
        Glide.with(requireContext()).load("https://github.com/saladnoober.png").into(binding.seventhIcon);

        binding.firstContributor.setOnClickListener(v -> openLink("https://github.com/syntaxspin"));
        binding.secondContributor.setOnClickListener(v -> openLink("https://github.com/trindadedev13"));
        binding.thirdContributor.setOnClickListener(v -> openLink("https://github.com/realekansh"));
        binding.fourthContributor.setOnClickListener(v -> openLink("https://github.com/masterzack69"));
        binding.bigThanks.setOnClickListener(v -> openLink("https://github.com/nift4"));
        binding.secondBigThanks.setOnClickListener(v -> openLink("https://github.com/piyush7890"));
        binding.firstThanks.setOnClickListener(v -> openLink("https://github.com/saladnoober"));

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.githubButton.setOnClickListener(v -> openLink(GITHUB_LINk));
        binding.telegramButton.setOnClickListener(v -> openLink(TELEGRAM_LINk));
    }

    private void openLink(String link) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(link));
        startActivity(intent);
    }
}

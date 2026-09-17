package com.xapps.media.xmusic.fragment;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.common.SettingsItem;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.utils.XUtils;

import java.util.ArrayList;
import java.util.List;

public class LyricsCustomizeFragment extends BasePrefsFragment {
     private RootActivity activity;

    @Override
    protected List<SettingsItem> provideItems() {
        activity = (RootActivity) getActivity();
        List<SettingsItem> settings = new ArrayList<>();

        settings.add(new SettingsItem(SettingsItem.TYPE_HEADER, "h1", "Lyrics font", "", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_NAV, "font_size", "Font size", "Choose how big lyrics text should be", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_SWITCH, "use_system_font", "Use System font", "Whether to use the app's font or the system's for rendering lyrics", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_NAV, "font_weight", "Font weight", "Choose the font weight that suits you", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_SWITCH, "rounded_font", "Round lyrics letters", "Make the lyrics letters' edges round", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_HEADER, "h2", "Lyrics Appearance", "", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_SWITCH, "enable_lyrics_gradient", "Enable lyrics gradient", "Display a beautiful gradient background behind the lyrics, might impact performance", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_SWITCH, "lyrics_blur", "Blur Lyrics", "Apply a blur effect for inactive lyrics lines, might impact performance", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_SWITCH, "lyrics_sparkles", "Enable Lyrics Sparkles", "Apply a Sparkling effect to the lyrics while being sung", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_HEADER, "h3", "Lyrics Behavior", "", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_SWITCH, "lyrics_anticipation", "Enable lyrics anticipation", "Lyrics will always go to next line before it's sung", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_SWITCH, "lyrics_elastic_scroll", "Enable Staggered Scrolling", "Make the lyrics lines scroll in a delayed way for better immersion", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_SWITCH, "lyrics_elastic_manual_scroll", "Staggered manual scrolling", "Apply the staggered scroll effect to user manual scrolling, might feel weird", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_HEADER, "h4", "Extra", "", null));
        settings.add(new SettingsItem(SettingsItem.TYPE_SWITCH, "lyrics_keep_screen_awake", "Keep screen awake", "If enabled, the screen will not turn off while lyrics are visible", null));

        return settings;
    }

    @Override
    public String getFragmentTitle() {
        return "Customize Lyrics";
    }

    @Override
    protected void onItemSelected(SettingsItem item) {
        switch (item.id) {
            case "font_size" -> {
                showFontSizeDialog();
            }
            case "font_weight" -> {
                showFontWeightDialog();
            }
            default -> {
                XUtils.showMessage(getActivity(), "Feature to be added soon");
            }
        }
    }

    private void showFontWeightDialog() {
        Context context = requireContext();
        float density = context.getResources().getDisplayMetrics().density;
        int paddingH = (int) (12 * density);
        int paddingT = (int) (16 * density);
        int paddingB = (int) (8 * density);
        int trackPadding = (int) (12 * density);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(paddingH, paddingT, paddingH, paddingB);

        Slider slider = new Slider(context);
        slider.setValueFrom(500f);
        slider.setValueTo(1000f);
        slider.setValue((float) DataManager.getLyricsWeight());
        slider.setStepSize(50f);
        slider.setLabelBehavior(LabelFormatter.LABEL_FLOATING);

        container.addView(slider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        RelativeLayout labelsLayout = new RelativeLayout(context);
        labelsLayout.setPadding(trackPadding, 0, trackPadding, 0);

        MaterialTextView minLabel = new MaterialTextView(context);
        minLabel.setText(String.valueOf((int) slider.getValueFrom()));
        minLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        RelativeLayout.LayoutParams minParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        minParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        labelsLayout.addView(minLabel, minParams);

        MaterialTextView maxLabel = new MaterialTextView(context);
        maxLabel.setText(String.valueOf((int) slider.getValueTo()));
        maxLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        RelativeLayout.LayoutParams maxParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        maxParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        labelsLayout.addView(maxLabel, maxParams);

        container.addView(labelsLayout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        assert getActivity() != null;
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle("Adjust Lyrics Font Weight")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    float finalValue = slider.getValue();
                    DataManager.setLyricsWeight((int) finalValue);
                    activity.updateLyrics();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showFontSizeDialog() {
        @NonNull Context context = requireContext();
        float density = context.getResources().getDisplayMetrics().density;
        int paddingH = (int) (12 * density);
        int paddingT = (int) (16 * density);
        int paddingB = (int) (8 * density);
        int trackPadding = (int) (12 * density);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(paddingH, paddingT, paddingH, paddingB);

        Slider slider = new Slider(context);
        slider.setValueFrom(12f);
        slider.setValueTo(40f);
        slider.setValue((float) DataManager.getLyricsSize());
        slider.setStepSize(2f);
        slider.setLabelBehavior(LabelFormatter.LABEL_FLOATING);

        container.addView(slider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        RelativeLayout labelsLayout = new RelativeLayout(context);
        labelsLayout.setPadding(trackPadding, 0, trackPadding, 0);

        MaterialTextView minLabel = new MaterialTextView(context);
        minLabel.setText(String.valueOf((int) slider.getValueFrom()) + "sp");
        minLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        RelativeLayout.LayoutParams minParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        minParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        labelsLayout.addView(minLabel, minParams);

        MaterialTextView maxLabel = new MaterialTextView(context);
        maxLabel.setText(String.valueOf((int) slider.getValueTo()) + "sp");
        maxLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        RelativeLayout.LayoutParams maxParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        maxParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        labelsLayout.addView(maxLabel, maxParams);

        container.addView(labelsLayout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        assert getActivity() != null;
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle("Adjust Lyrics Font Size")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    float finalValue = slider.getValue();
                    DataManager.setLyricsSize((int) finalValue);
                    activity.updateLyrics();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onSwitchChanged(SettingsItem item, boolean value) {
        super.onSwitchChanged(item, value);
        switch (item.id) {
            case "lyrics_elastic_scroll", "lyrics_elastic_manual_scroll", "lyrics_sparkles", "lyrics_anticipation", "enable_lyrics_gradient", "lyrics_blur", "lyrics_keep_screen_awake", "rounded_font", "use_system_font" -> {
                activity.updateLyrics();
            }
            default -> {
                XUtils.showMessage(getActivity(), "Feature to be added soon");
            }
        }
    }
}

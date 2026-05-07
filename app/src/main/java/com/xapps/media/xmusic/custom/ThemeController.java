package com.xapps.media.xmusic.custom;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.R;


public class ThemeController {
    private static ThemeController instance;
    private XResources globalResources;
    private int currentPrimary;

    private ThemeController() {}

    public static ThemeController getInstance() {
        if (instance == null) instance = new ThemeController();
        return instance;
    }

    public XResources getOrCreateResources(Resources base) {
        if (globalResources == null) {
            globalResources = new XResources(base);
        }
        return globalResources;
    }

    public void updateTheme(View rootView, int newColor) {
        this.currentPrimary = newColor;
        if (globalResources != null) {
            globalResources.setLiveColor(android.R.attr.colorPrimary, newColor);
            globalResources.setLiveColor(R.attr.colorOnPrimary, newColor);
        }
        refreshViews(rootView);
    }

    private void refreshViews(View view) {
        view.invalidate();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                refreshViews(group.getChildAt(i));
            }
        }
    }

    public int getCurrentPrimary() {
        return currentPrimary;
    }
}

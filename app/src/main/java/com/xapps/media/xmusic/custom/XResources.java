package com.xapps.media.xmusic.custom;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import java.util.HashMap;
import java.util.Map;

public class XResources extends Resources {
    private final Map<Integer, Integer> liveColors = new HashMap<>();

    public XResources(Resources res) {
        super(res.getAssets(), res.getDisplayMetrics(), res.getConfiguration());
    }

    public void setLiveColor(int resId, int color) {
        liveColors.put(resId, color);
    }

    @Override
    public int getColor(int id) throws NotFoundException {
        Integer color = liveColors.get(id);
        return (color != null) ? color : super.getColor(id);
    }

    @Override
    public int getColor(int id, Theme theme) throws NotFoundException {
        Integer color = liveColors.get(id);
        return (color != null) ? color : super.getColor(id, theme);
    }

    @Override
    public ColorStateList getColorStateList(int id) throws NotFoundException {
        Integer color = liveColors.get(id);
        return (color != null) ? ColorStateList.valueOf(color) : super.getColorStateList(id);
    }

    @Override
    public ColorStateList getColorStateList(int id, Theme theme) throws NotFoundException {
        Integer color = liveColors.get(id);
        return (color != null) ? ColorStateList.valueOf(color) : super.getColorStateList(id, theme);
    }
}

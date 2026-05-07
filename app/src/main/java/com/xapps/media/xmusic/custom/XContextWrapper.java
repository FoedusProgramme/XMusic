package com.xapps.media.xmusic.custom;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

public class XContextWrapper extends ContextWrapper {
    private final XResources xResources;

    public XContextWrapper(Context base, XResources res) {
        super(base);
        this.xResources = res;
    }

    @Override
    public Resources getResources() {
        return xResources;
    }
}

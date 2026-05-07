package com.xapps.media.xmusic.activity;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import com.xapps.media.xmusic.custom.ThemeController;
import com.xapps.media.xmusic.custom.XContextWrapper;
import com.xapps.media.xmusic.custom.XResources;


public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        XResources res = ThemeController.getInstance().getOrCreateResources(newBase.getResources());
        super.attachBaseContext(new XContextWrapper(newBase, res));
    }

    public void changeAppTheme(int color) {
        ThemeController.getInstance().updateTheme(getWindow().getDecorView(), color);
    }
}

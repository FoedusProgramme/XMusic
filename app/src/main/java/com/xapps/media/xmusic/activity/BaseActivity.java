package com.xapps.media.xmusic.activity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.XUtils;


public abstract class BaseActivity extends AppCompatActivity {
    @Override
	protected void onCreate(Bundle bundle) {
        initTheme();
        super.onCreate(bundle);
    }

    private void initTheme() {
        EdgeToEdge.enable(this);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        XUtils.updateTheme();
        XUtils.applyDynamicColors(this, DataManager.isOledThemeEnabled());
        if (XUtils.isDarkMode(this) && DataManager.isOledThemeEnabled())getTheme().applyStyle(R.style.ThemeOverlay_XMusic_OLED, true);
        MaterialColorUtils.initColors(this);
    }

    public void showInfoDialog(String title, int icon, String Desc, String button, View rootLayout) {
        MaterialAlertDialogBuilder m = new MaterialAlertDialogBuilder(this);
        m.setTitle(title);
        if (icon != 0) m.setIcon(icon);
        m.setMessage(Desc);
        m.setPositiveButton(button, (dialog, which) -> {
            dialog.dismiss();
        });
        m.setOnDismissListener(dialog -> {
            if (XUtils.areBlursOrDynamicColorsSupported() && DataManager.isBlurOn()) XUtils.animateBlur(rootLayout, false, 50);
        });
        m.show();
        if (XUtils.areBlursOrDynamicColorsSupported() && DataManager.isBlurOn()) XUtils.animateBlur(rootLayout, true, 300);
    }
}

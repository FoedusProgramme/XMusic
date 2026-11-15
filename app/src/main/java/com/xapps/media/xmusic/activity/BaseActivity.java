package com.xapps.media.xmusic.activity;
import com.xapps.media.xmusic.fragment.base.*;
import com.xapps.media.xmusic.fragment.main.*;
import com.xapps.media.xmusic.fragment.settings.*;
import com.xapps.media.xmusic.fragment.info.*;
import com.xapps.media.xmusic.ui.widget.*;
import com.xapps.media.xmusic.ui.lyrics.*;
import com.xapps.media.xmusic.ui.scroll.*;
import com.xapps.media.xmusic.ui.behavior.*;
import com.xapps.media.xmusic.lyrics.*;
import com.xapps.media.xmusic.callback.*;
import com.xapps.media.xmusic.models.*;

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

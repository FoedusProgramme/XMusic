package com.xapps.media.xmusic.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.transition.TransitionManager;

import com.google.android.material.transition.MaterialSharedAxis;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.common.SongLoadListener;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.fragment.MusicListFragment;
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.helper.SongSorter;
import com.xapps.media.xmusic.models.Song;
import com.xapps.media.xmusic.utils.XUtils;

import java.util.ArrayList;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 31) SplashScreen.installSplashScreen(this);

        SongMetadataHelper.getAllSongs(this, new SongLoadListener() {
            @Override
            public void onComplete(ArrayList<Song> list) {
                if (!XUtils.areAllPermsGranted(SplashActivity.this)) {
                    startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, RootActivity.class));
                }
            }
        });
        finish();
    }
}

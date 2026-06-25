package com.xapps.media.xmusic.activity;

import android.os.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.xapps.media.xmusic.activity.manager.LogicManager;
import com.xapps.media.xmusic.activity.manager.UIManager;
import com.xapps.media.xmusic.callback.ActivityCallback;
import com.xapps.media.xmusic.callback.CallbackInterface;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.models.Song;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import java.util.ArrayList;

public class RootActivity extends BaseActivity implements ActivityCallback {
    private ActivityRootBinding binding;
    private UIManager uiManager;
    private LogicManager logicManager;
    private MediaController mediaController;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRootBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }
    
    @Override 
    public void onStart() {
        super.onStart();
        CallbackInterface.setActivityCallback(this);
        logicManager.initController(this, controller -> {
                mediaController = controller;
                //progressDrawable.setAnimate(controller.isPlaying());
                //setupControllerListener();
            },
            e -> showInfoDialog("Error", 0, e.toString(), "OK", binding.Coordinator),
            this::restoreStateIfPossible
        );
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        CallbackInterface.clearActivityCallback(this);
        mediaController.release();
		mediaController = null;
    }

    private void init() {
        uiManager = new UIManager(this);
        uiManager.initUI();
        logicManager = new LogicManager(this, uiManager);
        logicManager.initLogic();
    }

    public ActivityRootBinding getBinding() {
        return binding;
    }

    public void setSong(int position) {
        logicManager.playSong(position);
        uiManager.hideComponents(false, uiManager.bnvHidden, uiManager.tabsHidden);
    }
    
    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        logicManager.updateVumeters(isPlaying);
    }

    public MediaController getController() {
        return mediaController;
    }

    public void updateSongsQueue(ArrayList<Song> songs) {
        if (CallbackInterface.service() != null) CallbackInterface.service().updateSongs();
    }

    public void restoreStateIfPossible() {
        
    }

    public void loadSettings() {
        
    }
    
    @Override
    public void onColorsChanged() {
        runOnUiThread(() -> uiManager.updateColors());
    }
}

package com.xapps.media.xmusic.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.xapps.media.xmusic.activity.manager.LogicManager;
import com.xapps.media.xmusic.activity.manager.UIManager;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.utils.MaterialColorUtils;

public class RootActivity extends BaseActivity {
    private ActivityRootBinding binding;
    private UIManager uiManager;
    private LogicManager logicManager;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRootBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
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
}

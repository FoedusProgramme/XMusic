package com.xapps.media.xmusic.activity.manager;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.utils.XUtils;
import com.xapps.media.xmusic.widget.ExpressiveSliderLayout;

public class LogicManager {
    private RootActivity activity;
    private ActivityRootBinding binding;
    private UIManager uiManager;
    
    private boolean test1 = false;
    private boolean test2 = false;

    public LogicManager(RootActivity activity, UIManager uiManager) {
        this.activity = activity;
        this.binding = activity.getBinding();
        this.uiManager = uiManager;
    }

    public void initLogic() {
        setupListeners();
        setupCallbacks();
    }

    private void setupListeners() {
        binding.bottomButton.setOnClickListener(v -> {
            if (test1) {
                if (test2) {
                    uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_TABS_BNV);
                } else {
                    uiManager.setLayoutState(UIManager.LAYOUT_STATE_FULL);
                }
                test1 = false;
                binding.bottomButton.setText("Show Player");
            } else {
                if (test2) {
                    uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_FULL);
                } else {
                    uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_PLAYER_ONLY);
                }
                test1 = true;
                binding.bottomButton.setText("Hide Player");
            }
        });
        
        binding.topButton.setOnClickListener(v -> {
            if (test2) {
                if (test1) {
                    uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_PLAYER_ONLY);
                } else {
                    uiManager.setLayoutState(UIManager.LAYOUT_STATE_FULL);
                }
                test2 = false;
                binding.topButton.setText("Expand Card");
            } else {
                if (test1) {
                    uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_FULL);
                } else {
                    uiManager.setLayoutState(UIManager.LAYOUT_STATE_EXPOSE_TABS_BNV);
                }
                test2 = true;
                binding.topButton.setText("Collapse Card");
            }
        });
    }

    private void setupCallbacks() {
        binding.miniPlayer.addSliderCallback(new ExpressiveSliderLayout.SliderCallback() {
            @Override
            public void onStateChanged(int state) {
                
            }
            
            @Override
            public void onSlide(float offset) {
                binding.layoutScrim.setAlpha(Math.max(0f, offset));
            }
        });
    }
}
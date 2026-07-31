package com.xapps.media.xmusic.callback;
import com.google.android.material.search.SearchView;
import com.xapps.media.xmusic.activity.manager.UIManager;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;

public interface FragmentCallback {
    default void updateVumeter(boolean b) {
        
    }

    default void updateActiveItem(int i) {
        
    }

    default void freeze(boolean f) {
        
    }
    
    default SearchView.TransitionState getSearchViewState() {
        return null;
    }
    
    default void hideSearchView() {
        
    }
    
    default int getLayoutState() {
        return UIManager.LAYOUT_STATE_EXPOSE_BNV;
    }

    default void onBindingReady(ActivityRootBinding binding) {
        
    }
} 

package com.xapps.media.xmusic.helper;

import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.xapps.media.xmusic.widget.ExpressiveSliderLayout;

public class FabPlacementHelper implements DefaultLifecycleObserver {

    private final ExtendedFloatingActionButton fab;
    private final ExpressiveSliderLayout bottomSheet;
    @Nullable private final View bottomNavigationView;
    @Nullable private final RecyclerView recyclerView;
    private final float marginPx;

    private ExpressiveSliderLayout.SliderCallback sheetCallback;
    private RecyclerView.OnScrollListener scrollListener;

    private final int[] bnvLoc = new int[2];
    private final int[] sheetLoc = new int[2];
    private final int[] fabLoc = new int[2];

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        updateTranslationFrameByFrame();
        return true;
    };

    public FabPlacementHelper(@NonNull ExtendedFloatingActionButton fab, 
                              @NonNull ExpressiveSliderLayout bottomSheet, 
                              @Nullable View bottomNavigationView,
                              @Nullable RecyclerView recyclerView) {
        this.fab = fab;
        this.bottomSheet = bottomSheet;
        this.bottomNavigationView = bottomNavigationView;
        this.recyclerView = recyclerView;
        
        float density = fab.getContext().getResources().getDisplayMetrics().density;
        this.marginPx = 24f * density;
    }

    public void wireUp(@NonNull LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().addObserver(this);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        sheetCallback = new ExpressiveSliderLayout.SliderCallback() {
            @Override
            public void onStateChanged(int newState) {
                if (newState == ExpressiveSliderLayout.STATE_COLLAPSED || newState == ExpressiveSliderLayout.STATE_HIDDEN) {
                    if (fab.getVisibility() != View.VISIBLE) {
                        fab.show();
                    }
                } else if (newState == ExpressiveSliderLayout.STATE_EXPANDED) {
                    if (fab.getVisibility() == View.VISIBLE) {
                        fab.hide();
                    }
                }
            }

            @Override
            public void onSlide(float slideOffset) {
                if (slideOffset > 0f) {
                    if (fab.getVisibility() == View.VISIBLE) {
                        fab.hide();
                    }
                } else {
                    if (fab.getVisibility() != View.VISIBLE) {
                        fab.show();
                    }
                }
            }
        };

        if (recyclerView != null) {
            scrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    int state = bottomSheet.getState();
                    if (state == ExpressiveSliderLayout.STATE_EXPANDED || state == ExpressiveSliderLayout.STATE_DRAGGING) {
                        return;
                    }

                    if (dy > 10) {
                        fab.shrink();
                    } else if (dy < -10) {
                        fab.extend();
                    }
                }
            };
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (bottomSheet != null && sheetCallback != null) {
            bottomSheet.addSliderCallback(sheetCallback);
        }
        
        if (recyclerView != null && scrollListener != null) {
            recyclerView.addOnScrollListener(scrollListener);
        }
        
        int state = bottomSheet.getState();
        if (state == ExpressiveSliderLayout.STATE_EXPANDED) {
            fab.hide();
        } else {
            fab.show();
        }
        
        fab.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (bottomSheet != null && sheetCallback != null) {
            bottomSheet.removeSliderCallback(sheetCallback);
        }
        
        if (recyclerView != null && scrollListener != null) {
            recyclerView.removeOnScrollListener(scrollListener);
        }
        
        fab.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        sheetCallback = null;
        scrollListener = null;
    }

    private void updateTranslationFrameByFrame() {
        if (fab == null || bottomSheet == null) return;

        float baselineY = ((View) bottomSheet.getParent()).getHeight();

        if (bottomNavigationView != null && bottomNavigationView.getVisibility() == View.VISIBLE) {
            bottomNavigationView.getLocationInWindow(bnvLoc);
            baselineY = bnvLoc[1];
        }

        View sheetChild = bottomSheet.getChildCount() > 0 ? bottomSheet.getChildAt(0) : null;
        float sheetTop = baselineY;
        if (sheetChild != null) {
            sheetChild.getLocationInWindow(sheetLoc);
            sheetTop = sheetLoc[1];
        }

        int state = bottomSheet.getState();
        float targetBoundaryY;

        if (state == ExpressiveSliderLayout.STATE_HIDDEN) {
            targetBoundaryY = baselineY;
        } else {
            targetBoundaryY = Math.min(baselineY, sheetTop);
        }

        fab.getLocationInWindow(fabLoc);
        float currentTransY = fab.getTranslationY();
        float baseFabTop = fabLoc[1] - currentTransY;
        float baseFabBottom = baseFabTop + fab.getHeight();

        float targetFabBottom = targetBoundaryY - marginPx;
        float neededTranslation = targetFabBottom - baseFabBottom;

        if (Math.abs(currentTransY - neededTranslation) > 0.5f) {
            fab.setTranslationY(neededTranslation);
        }
    }
}

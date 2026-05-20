package com.xapps.media.xmusic.helper;

import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.xapps.media.xmusic.models.BottomSheetBehavior;

public class FabPlacementHelper implements DefaultLifecycleObserver {

    private final ExtendedFloatingActionButton fab;
    private final View bottomSheet;
    @Nullable private final View bottomNavigationView;
    @Nullable private final RecyclerView recyclerView;
    private final float marginPx;

    private BottomSheetBehavior<?> bottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback sheetCallback;
    private RecyclerView.OnScrollListener scrollListener;

    private final int[] bnvLoc = new int[2];
    private final int[] sheetLoc = new int[2];
    private final int[] fabLoc = new int[2];

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        updateTranslationFrameByFrame();
        return true;
    };

    public FabPlacementHelper(@NonNull ExtendedFloatingActionButton fab, 
                              @NonNull View bottomSheet, 
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
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        
        sheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (fab.getVisibility() != View.VISIBLE) {
                        fab.show();
                    }
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (fab.getVisibility() == View.VISIBLE) {
                        fab.hide();
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
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
                    if (bottomSheetBehavior != null) {
                        int state = bottomSheetBehavior.getState();
                        if (state == BottomSheetBehavior.STATE_EXPANDED || state == BottomSheetBehavior.STATE_DRAGGING) {
                            return;
                        }
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
        if (bottomSheetBehavior != null && sheetCallback != null) {
            bottomSheetBehavior.addBottomSheetCallback(sheetCallback);
        }
        
        if (recyclerView != null && scrollListener != null) {
            recyclerView.addOnScrollListener(scrollListener);
        }
        
        int state = bottomSheetBehavior.getState();
        if (state == BottomSheetBehavior.STATE_EXPANDED) {
            fab.hide();
        } else {
            fab.show();
        }
        
        fab.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (bottomSheetBehavior != null && sheetCallback != null) {
            bottomSheetBehavior.removeBottomSheetCallback(sheetCallback);
        }
        
        if (recyclerView != null && scrollListener != null) {
            recyclerView.removeOnScrollListener(scrollListener);
        }
        
        fab.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        bottomSheetBehavior = null;
        sheetCallback = null;
        scrollListener = null;
    }

    private void updateTranslationFrameByFrame() {
        if (fab == null || bottomSheet == null || bottomSheetBehavior == null) return;

        float baselineY = ((View) bottomSheet.getParent()).getHeight();

        if (bottomNavigationView != null && bottomNavigationView.getVisibility() == View.VISIBLE) {
            bottomNavigationView.getLocationInWindow(bnvLoc);
            baselineY = bnvLoc[1];
        }

        bottomSheet.getLocationInWindow(sheetLoc);
        float sheetTop = sheetLoc[1];

        int state = bottomSheetBehavior.getState();
        float targetBoundaryY;

        if (state == BottomSheetBehavior.STATE_HIDDEN) {
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

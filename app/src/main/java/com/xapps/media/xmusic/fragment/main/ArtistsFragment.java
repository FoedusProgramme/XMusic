package com.xapps.media.xmusic.fragment.main;

import com.xapps.media.xmusic.fastscroll.*;
import com.xapps.media.xmusic.fragment.base.*;
import com.xapps.media.xmusic.fragment.settings.*;
import com.xapps.media.xmusic.fragment.info.*;
import com.xapps.media.xmusic.ui.widget.*;
import com.xapps.media.xmusic.ui.lyrics.*;
import com.xapps.media.xmusic.ui.scroll.*;
import com.xapps.media.xmusic.ui.behavior.*;
import com.xapps.media.xmusic.lyrics.*;
import com.xapps.media.xmusic.callback.*;
import com.xapps.media.xmusic.models.*;

import android.annotation.SuppressLint;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedDispatcher;
import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.*;
import androidx.core.content.*;
import androidx.core.view.ViewKt;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialSplitButton;
import com.google.android.material.transition.MaterialSharedAxis;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.callback.FragmentCallback;
import com.xapps.media.xmusic.callback.ArtistLoadListener;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.databinding.*;
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.helper.ArtistSorter;
import com.xapps.media.xmusic.models.Artist;
import com.xapps.media.xmusic.utils.*;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.Unit;

public class ArtistsFragment extends BaseFragment implements FragmentCallback {

    public FragmentArtistsBinding binding;
    public final Fragment f = this;
    public int imageSize, size, statusBarHeight;
    private ActivityRootBinding activity;
    private RootActivity a;
    private ArtistsListAdapter artistsAdapter;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Handler mainHandler;
    private Drawable placeholder;
    private ConcatAdapter concatAdapter;
    private FastScroller scroller;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
        a = (RootActivity) getActivity();
        binding = FragmentArtistsBinding.inflate(_inflater, _container, false);
        mainHandler = new Handler(Looper.getMainLooper());
        initializeLogic();
        setUpListeners();
        return binding.getRoot();
    }

    private void initializeLogic() {
        loadArtists();
        assert getActivity() != null;
        statusBarHeight = XUtils.getStatusBarHeight(getActivity());
        activity = a.getBinding();
        placeholder = ContextCompat.getDrawable(getActivity(), R.drawable.artist_placeholder);
        imageSize = XUtils.convertToPx(getActivity(), 60f);
        ViewKt.doOnLayout(activity.bottomNavigation, v -> {
            if (getActivity() == null) return Unit.INSTANCE;
            int lastSpacing = activity.bottomNavigation.getHeight() * 2;
            binding.artistsList.addItemDecoration(new BottomSpacingDecoration(lastSpacing));
            return Unit.INSTANCE;
        });

        binding.artistsList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        activity = null;
        a = null;
    }

    public void setUpListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (scroller != null) scroller.setForceHidden(true);
            SongMetadataHelper.clearCachedList();
            loadArtists();
        });
    }

    public class ArtistsListAdapter extends RecyclerView.Adapter<ArtistsListAdapter.ViewHolder> {

        int c3 = MaterialColorUtils.colorOnSurface;
        int c4 = MaterialColorUtils.colorOutline;

        private ArrayList<Artist> data;

        private static final int TYPE_SINGLE = -1;
        private static final int TYPE_TOP = 0;
        private static final int TYPE_MIDDLE = 1;
        private static final int TYPE_BOTTOM = 2;

        public ArtistsListAdapter(Context c, ArrayList<Artist> arraylist) {
            data = arraylist;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int layout;
            if (viewType == TYPE_TOP) layout = R.layout.artist_item_top;
            else if (viewType == TYPE_BOTTOM) layout = R.layout.artist_item_bottom;
            else if (viewType == TYPE_SINGLE) layout = R.layout.artist_item_single;
            else layout = R.layout.artist_item_middle;

            return new ViewHolder(inflater.inflate(layout, parent, false));
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            View view = holder.itemView;
            ArtistItemMiddleBinding itemBinding = ArtistItemMiddleBinding.bind(view);

            itemBinding.item.setChecked(false);
            itemBinding.vumeterFrame.setVisibility(View.INVISIBLE);
            itemBinding.artistName.setTextColor(c3);
            itemBinding.artistInfo.setTextColor(c4);

            String title = data.get(position).name;
            int tracks = data.get(position).numberOfTracks;
            int albums = data.get(position).numberOfAlbums;

            Uri coverUri = null;
            for (com.xapps.media.xmusic.models.Song s : RuntimeData.songs) {
                if (title != null && title.equals(s.artist)) {
                    coverUri = s.getArtworkUri();
                    break;
                }
            }

            Glide.with(f)
                    .load(coverUri)
                    .centerCrop()
                    .fallback(placeholder)
                    .error(placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(placeholder)
                    .override(imageSize, imageSize)
                    .into(itemBinding.artistCover);

            if (title == null || title.isEmpty()) {
                itemBinding.artistName.setText(getString(R.string.unknown));
            } else {
                itemBinding.artistName.setText(title);
                String trackStr = tracks + " " + (tracks == 1 ? "Track" : "Tracks");
                String albumStr = albums + " " + (albums == 1 ? "Album" : "Albums");
                itemBinding.artistInfo.setText(trackStr + " • " + albumStr);
            }

            itemBinding.item.setOnClickListener(v -> {
            });
            itemBinding.optionsIcon.setOnClickListener(v -> {
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public long getItemId(int position) {
            return data.get(position).id;
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            View v = holder.itemView.findViewById(R.id.artistCover);
            Glide.with(holder.itemView.getContext()).clear(v);
        }

        @Override
        public int getItemViewType(int position) {
            int size = getItemCount();

            if (size == 1) {
                return TYPE_SINGLE;
            }

            if (size == 2) {
                return position == 0 ? TYPE_TOP : TYPE_BOTTOM;
            }

            if (position == 0) return TYPE_TOP;
            if (position == size - 1) return TYPE_BOTTOM;
            return TYPE_MIDDLE;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }

        public void updateData(ArrayList<Artist> a) {
            data = a;
            notifyDataSetChanged();
        }
    }

    public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder> {
        @NonNull
        @Override
        public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View headerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_view, parent, false);
            return new HeaderViewHolder(headerView);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
            activity.bottomNavigation.post(() -> {
                if (scroller == null) {
                    assert getActivity() != null;
                    scroller = new FastScrollerBuilder(binding.artistsList).useMd2Style().setPopupStyle(t -> {
                        t.setTextColor(MaterialColorUtils.colorOnPrimaryContainer);
                        t.setTextSize(XUtils.convertSpToPx(requireContext(), 18f));
                        t.setGravity(Gravity.CENTER);
                        t.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.shape_clover));
                    }).setPopupTextProvider((view, position2) -> {
                        if (position2 <= 0 || position2 - 1 >= RuntimeData.artists.size()) {
                            return "—";
                        }

                        Artist artist = RuntimeData.artists.get(position2 - 1);
                        String name = artist.name;

                        if (name == null || name.isEmpty()) {
                            return "#";
                        }

                        return String.valueOf(Character.toUpperCase(name.charAt(0)));
                    }).setPadding(0, XUtils.convertToPx(getActivity(), 8f) + holder.itemView.getHeight(), 0, XUtils.convertToPx(getActivity(), 8f)).build();
                }
            });
            View view = holder.itemView;
            TextView sg = view.findViewById(R.id.songs_count);
            sg.setText(getString(R.string.artists_count_format, size));
            MaterialButton orderButton = view.findViewById(R.id.order_type_button);
            orderButton.setChecked(!DataManager.isArtistDescendingOrder());
            MaterialButton filterButton = view.findViewById(R.id.sort_filter_button);
            MaterialSplitButton msb = view.findViewById(R.id.split_button);
            orderButton.setOnClickListener(v -> {
                boolean b = orderButton.isChecked();
                DataManager.setArtistDescendingOrder(!b);
                ArtistSorter.sort(RuntimeData.artists, DataManager.getArtistFilterType(), !b, sorted -> {
                    RuntimeData.artists = sorted;
                    artistsAdapter.updateData(sorted);
                });
            });
            filterButton.setOnClickListener(v -> {
                LayoutArtistSortPopupBinding b = LayoutArtistSortPopupBinding.inflate(getLayoutInflater());

                final boolean[] isDismissing = new boolean[]{false};
                final SpringAnimation[] activeSprings = new SpringAnimation[2];

                int cardWidth = XUtils.convertToPx(requireContext(), 230f);
                b.popupCard.measure(
                        View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );

                final PopupWindow[] popupHolder = new PopupWindow[1];
                final OnBackAnimationCallback[] windowBackCallback = new OnBackAnimationCallback[1];

                Runnable onBackStartLogic = () -> {
                    if (isDismissing[0]) return;
                    if (activeSprings[0] != null && activeSprings[0].isRunning()) activeSprings[0].cancel();
                    if (activeSprings[1] != null && activeSprings[1].isRunning()) activeSprings[1].cancel();
                    b.popupCard.animate().cancel();
                };

                java.util.function.Consumer<Float> onBackProgressLogic = progress -> {
                    if (isDismissing[0]) return;
                    if (activeSprings[0] != null && activeSprings[0].isRunning()) activeSprings[0].cancel();
                    if (activeSprings[1] != null && activeSprings[1].isRunning()) activeSprings[1].cancel();
                    b.popupCard.animate().cancel();

                    float scale = Math.max(0.6f, 1f - (0.4f * progress));
                    float alpha = Math.max(0f, 1f - progress);

                    b.popupCard.setPivotX(cardWidth);
                    b.popupCard.setPivotY(0f);
                    b.popupCard.setScaleX(scale);
                    b.popupCard.setScaleY(scale);
                    b.popupCard.setAlpha(alpha);
                };

                Runnable onBackCancelLogic = () -> {
                    if (isDismissing[0]) return;
                    b.popupCard.setPivotX(cardWidth);
                    b.popupCard.setPivotY(0f);
                    b.popupCard.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(160)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                };

                Runnable onBackInvokeLogic = () -> {
                    if (isDismissing[0]) return;
                    if (popupHolder[0] != null) popupHolder[0].dismiss();
                };

                final OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackStarted(@NonNull BackEventCompat backEvent) {
                        onBackStartLogic.run();
                    }

                    @Override
                    public void handleOnBackProgressed(@NonNull BackEventCompat backEvent) {
                        onBackProgressLogic.accept(backEvent.getProgress());
                    }

                    @Override
                    public void handleOnBackPressed() {
                        onBackInvokeLogic.run();
                    }

                    @Override
                    public void handleOnBackCancelled() {
                        onBackCancelLogic.run();
                    }
                };

                android.widget.PopupWindow popup = new android.widget.PopupWindow(
                        b.getRoot(),
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        true
                ) {
                    @Override
                    public void dismiss() {
                        if (!isDismissing[0]) {
                            isDismissing[0] = true;
                            backCallback.setEnabled(false);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && windowBackCallback[0] != null) {
                                OnBackInvokedDispatcher dispatcher = b.getRoot().findOnBackInvokedDispatcher();
                                if (dispatcher != null) {
                                    dispatcher.unregisterOnBackInvokedCallback(windowBackCallback[0]);
                                }
                                windowBackCallback[0] = null;
                            }
                            if (activeSprings[0] != null && activeSprings[0].isRunning()) activeSprings[0].cancel();
                            if (activeSprings[1] != null && activeSprings[1].isRunning()) activeSprings[1].cancel();

                            if (DataManager.isBlurOn() && XUtils.areBlursOrDynamicColorsSupported()) {
                                XUtils.animateBlur(activity.Coordinator, false, 160);
                            }

                            b.popupCard.setPivotX(cardWidth);
                            b.popupCard.setPivotY(0f);
                            b.popupCard.animate()
                                    .alpha(0f)
                                    .scaleX(0.7f)
                                    .scaleY(0.7f)
                                    .setDuration(160)
                                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                                    .setListener(new android.animation.AnimatorListenerAdapter() {
                                        private boolean finished = false;
                                        @Override
                                        public void onAnimationEnd(android.animation.Animator animation) {
                                            finishDismiss();
                                        }
                                        @Override
                                        public void onAnimationCancel(android.animation.Animator animation) {
                                            finishDismiss();
                                        }
                                        private void finishDismiss() {
                                            if (!finished) {
                                                finished = true;
                                                try {
                                                    superDismiss();
                                                } catch (Exception ignored) {}
                                            }
                                        }
                                    })
                                    .start();
                        } else {
                            super.dismiss();
                        }
                    }

                    private void superDismiss() {
                        super.dismiss();
                    }
                };
                popupHolder[0] = popup;

                popup.setOnDismissListener(() -> {
                    backCallback.setEnabled(false);
                    backCallback.remove();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && windowBackCallback[0] != null) {
                        OnBackInvokedDispatcher dispatcher = b.getRoot().findOnBackInvokedDispatcher();
                        if (dispatcher != null) {
                            dispatcher.unregisterOnBackInvokedCallback(windowBackCallback[0]);
                        }
                        windowBackCallback[0] = null;
                    }
                });

                popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popup.setElevation(0f);
                popup.setFocusable(true);
                popup.setOutsideTouchable(false);
                popup.setClippingEnabled(false);
                popup.setAnimationStyle(0);

                boolean isAlpha = DataManager.getArtistFilterType() == ArtistSorter.SortBy.ALPHABETIC;
                b.radioSortAlpha.setChecked(isAlpha);
                b.cardSortAlpha.setChecked(isAlpha);
                b.textSortAlpha.setTextColor(isAlpha ? MaterialColorUtils.colorPrimary : MaterialColorUtils.colorOnSurface);

                b.radioSortCount.setChecked(!isAlpha);
                b.cardSortCount.setChecked(!isAlpha);
                b.textSortCount.setTextColor(!isAlpha ? MaterialColorUtils.colorPrimary : MaterialColorUtils.colorOnSurface);

                b.cardSortAlpha.setOnClickListener(v2 -> {
                    if (isDismissing[0]) return;
                    b.radioSortAlpha.setChecked(true);
                    b.cardSortAlpha.setChecked(true);
                    b.textSortAlpha.setTextColor(MaterialColorUtils.colorPrimary);
                    b.radioSortCount.setChecked(false);
                    b.cardSortCount.setChecked(false);
                    b.textSortCount.setTextColor(MaterialColorUtils.colorOnSurface);
                    DataManager.setArtistFilterType(ArtistSorter.SortBy.ALPHABETIC);
                    ArtistSorter.sort(RuntimeData.artists, ArtistSorter.SortBy.ALPHABETIC, DataManager.isArtistDescendingOrder(), sortedMap -> {
                        RuntimeData.artists = sortedMap;
                        artistsAdapter.updateData(sortedMap);
                    });
                    popup.dismiss();
                });
                b.cardSortCount.setOnClickListener(v2 -> {
                    if (isDismissing[0]) return;
                    b.radioSortAlpha.setChecked(false);
                    b.cardSortAlpha.setChecked(false);
                    b.textSortAlpha.setTextColor(MaterialColorUtils.colorOnSurface);
                    b.radioSortCount.setChecked(true);
                    b.cardSortCount.setChecked(true);
                    b.textSortCount.setTextColor(MaterialColorUtils.colorPrimary);
                    DataManager.setArtistFilterType(ArtistSorter.SortBy.SONG_COUNT);
                    ArtistSorter.sort(RuntimeData.artists, ArtistSorter.SortBy.SONG_COUNT, DataManager.isArtistDescendingOrder(), sortedMap -> {
                        RuntimeData.artists = sortedMap;
                        artistsAdapter.updateData(sortedMap);
                    });
                    popup.dismiss();
                });

                final boolean[] downOutside = new boolean[1];

                b.getRoot().setOnTouchListener((v2, event) -> {
                    if (isDismissing[0]) return true;

                    Rect cardRect = new Rect();
                    b.popupCard.getHitRect(cardRect);
                    float curX = event.getX();
                    float curY = event.getY();

                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downOutside[0] = !cardRect.contains((int) curX, (int) curY);
                            return downOutside[0];

                        case MotionEvent.ACTION_MOVE:
                            if (downOutside[0]) {
                                return true;
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                            if (downOutside[0]) {
                                boolean upOutside = !cardRect.contains((int) curX, (int) curY);
                                if (upOutside) {
                                    popup.dismiss();
                                }
                                downOutside[0] = false;
                                return true;
                            }
                            break;

                        case MotionEvent.ACTION_CANCEL:
                            downOutside[0] = false;
                            break;
                    }
                    return false;
                });

                b.getRoot().setFocusableInTouchMode(true);

                if (DataManager.isBlurOn() && XUtils.areBlursOrDynamicColorsSupported()) XUtils.animateBlur(activity.Coordinator, true, 300);

                View anchorView = filterButton;
                int[] location = new int[2];
                anchorView.getLocationInWindow(location);
                int anchorRight = location[0] + anchorView.getWidth();
                int anchorBottom = location[1] + anchorView.getHeight();

                int gap = XUtils.convertToPx(requireContext(), 4f);
                int targetX = anchorRight - cardWidth;
                int targetY = anchorBottom + gap;

                int screenMargin = XUtils.convertToPx(requireContext(), 8f);
                if (targetX < screenMargin) {
                    targetX = screenMargin;
                }

                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) b.popupCard.getLayoutParams();
                if (lp == null) {
                    lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.leftMargin = targetX;
                lp.topMargin = targetY;
                b.popupCard.setLayoutParams(lp);

                b.popupCard.setPivotX(cardWidth);
                b.popupCard.setPivotY(0f);
                b.popupCard.setScaleX(0.7f);
                b.popupCard.setScaleY(0.7f);
                b.popupCard.setAlpha(0f);

                a.getOnBackPressedDispatcher().addCallback(backCallback);
                popup.showAtLocation(anchorView, Gravity.NO_GRAVITY, 0, 0);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    b.getRoot().post(() -> {
                        if (isDismissing[0]) return;
                        OnBackInvokedDispatcher dispatcher = b.getRoot().findOnBackInvokedDispatcher();
                        if (dispatcher != null) {
                            windowBackCallback[0] = new OnBackAnimationCallback() {
                                @Override
                                public void onBackStarted(@NonNull BackEvent backEvent) {
                                    onBackStartLogic.run();
                                }

                                @Override
                                public void onBackProgressed(@NonNull BackEvent backEvent) {
                                    onBackProgressLogic.accept(backEvent.getProgress());
                                }

                                @Override
                                public void onBackInvoked() {
                                    onBackInvokeLogic.run();
                                }

                                @Override
                                public void onBackCancelled() {
                                    onBackCancelLogic.run();
                                }
                            };
                            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, windowBackCallback[0]);
                        }
                    });
                }

                SpringForce springX = new SpringForce(1f)
                        .setDampingRatio(0.40f)
                        .setStiffness(600f);

                SpringForce springY = new SpringForce(1f)
                        .setDampingRatio(0.40f)
                        .setStiffness(600f);

                SpringAnimation animX = new SpringAnimation(b.popupCard, DynamicAnimation.SCALE_X);
                animX.setSpring(springX);
                animX.setStartValue(0.7f);

                SpringAnimation animY = new SpringAnimation(b.popupCard, DynamicAnimation.SCALE_Y);
                animY.setSpring(springY);
                animY.setStartValue(0.7f);

                activeSprings[0] = animX;
                activeSprings[1] = animY;

                b.popupCard.animate().alpha(1f).setDuration(150).start();
                animX.start();
                animY.start();
            });

        }

        @Override
        public int getItemCount() {
            return 1;
        }
        class HeaderViewHolder extends RecyclerView.ViewHolder {
            public HeaderViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    public class BottomSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int bottomSpacing;
        private int spacing;
        private int sideSpacing;
        public BottomSpacingDecoration(int bottomSpacing) {
            sideSpacing = XUtils.convertToPx(getActivity(), 12f);
            this.bottomSpacing = bottomSpacing;
            spacing = XUtils.convertToPx(getActivity(), 2f);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) return;
            if (position == state.getItemCount() - 1) {
                outRect.set(sideSpacing, 0, sideSpacing, bottomSpacing);
            } else {
                outRect.set(sideSpacing, 0, sideSpacing, spacing);
            }
        }
    }

    private void loadArtists() {
        executor.execute(() -> {
            SongMetadataHelper.getAllArtists(getActivity(), ArtistLoadListener.on(list -> {
                if (getActivity() == null) return;
                ArtistSorter.sort(list, DataManager.getArtistFilterType(), DataManager.isArtistDescendingOrder(), sortedList -> {
                    RuntimeData.artists = sortedList;
                    size = RuntimeData.artists.size();
                    artistsAdapter = new ArtistsListAdapter(getActivity(), RuntimeData.artists);
                    HeaderAdapter headerAdapter = new HeaderAdapter();
                    concatAdapter = new ConcatAdapter(headerAdapter, artistsAdapter);
                    mainHandler.post(() -> {
                        binding.artistsList.setAdapter(concatAdapter);
                        mainHandler.postDelayed(() -> binding.swipeRefreshLayout.setRefreshing(false), 1000);
                        if (scroller != null) scroller.setForceHidden(false);
                        binding.artistsList.setItemAnimator(null);
                        MaterialSharedAxis msa = new MaterialSharedAxis(MaterialSharedAxis.Y, true);
                        msa.setDuration(500);
                        if (binding.emptyLayout.getVisibility() == View.VISIBLE) TransitionManager.beginDelayedTransition(binding.coordinator, msa);
                        binding.emptyLayout.setVisibility(View.GONE);
                        binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
                    });
                });
            }, e -> {
                Log.e("ArtistsFragment", "Failed to load artists", e);
            }));
        });
    }

    @Override
    public void freeze(boolean b) {
        binding.blockingOverlay.setClickable(b);
    }

    @Override
    public void updateActiveItem(int i) {
    }
}
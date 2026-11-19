package com.xapps.media.xmusic.fragment.info;

import com.xapps.media.xmusic.fragment.base.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewKt;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;
import androidx.transition.TransitionManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.transition.MaterialContainerTransform;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.entity.Library;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.databinding.*;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.utils.XUtils;

import java.util.List;
import kotlin.Unit;

public class UsedLibsFragment extends BaseFragment {

    private FragmentUsedLibsBinding binding;
    private RootActivity activity;

    private View expandedItemView;
    private float currentCornerRadius = 0f;
    private boolean isClosing = false;

    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackStarted(@NonNull BackEventCompat backEvent) {
            binding.detailsFragmentContainer.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), currentCornerRadius);
                }
            });
            binding.detailsFragmentContainer.setClipToOutline(true);
        }

        @Override
        public void handleOnBackProgressed(@NonNull BackEventCompat backEvent) {
            float progress = backEvent.getProgress();
            float scale = 1.0f - (progress * 0.15f);
            currentCornerRadius = progress * XUtils.convertToPx(requireContext(), 28f);

            float maxShiftX = XUtils.convertToPx(requireContext(), 12f);
            float shiftX = backEvent.getSwipeEdge() == BackEventCompat.EDGE_LEFT
                    ? progress * maxShiftX
                    : -progress * maxShiftX;

            binding.detailsFragmentContainer.setScaleX(scale);
            binding.detailsFragmentContainer.setScaleY(scale);
            binding.detailsFragmentContainer.setTranslationX(shiftX);
            binding.detailsFragmentContainer.invalidateOutline();
        }

        @Override
        public void handleOnBackPressed() {
            closeFragment();
        }

        @Override
        public void handleOnBackCancelled() {
            binding.detailsFragmentContainer.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        currentCornerRadius = 0f;
                        binding.detailsFragmentContainer.setClipToOutline(false);
                        binding.detailsFragmentContainer.invalidateOutline();
                    })
                    .start();
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUsedLibsBinding.inflate(inflater, container, false);
        activity = (RootActivity) getActivity();
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    private void init() {
        ViewKt.doOnLayout(activity.getBinding().bottomNavigation, v -> {
            try {
                java.io.InputStream is = requireContext().getResources().openRawResource(R.raw.aboutlibraries);
                @SuppressWarnings("SpellCheckingInspection") java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, length);
                }
                is.close();
                @SuppressWarnings("CharsetObjectCanBeUsed") Libs libs = new Libs.Builder().withJson(baos.toString("UTF-8")).build();
                List<Library> list = libs.getLibraries();
                ListAdapter adapter = new ListAdapter(list);
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                binding.recyclerView.setAdapter(adapter);
                binding.recyclerView.addItemDecoration(new SpacingDecoration(activity.getBinding().bottomNavigation.getHeight() * 2));
            } catch (Exception e) {
                Log.e("UsedLibsFragment:init", e.toString());
            }
            return Unit.INSTANCE;
        });
    }

    public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

        private final List<Library> libsList;

        private static final int TYPE_TOP = 0;
        private static final int TYPE_MIDDLE = 1;
        private static final int TYPE_BOTTOM = 2;

        public ListAdapter(List<Library> list) {
            libsList = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int layout;

            if (viewType == TYPE_TOP) layout = R.layout.libraries_item_layout_top;
            else if (viewType == TYPE_BOTTOM) layout = R.layout.libraries_item_layout_bottom;
            else layout = R.layout.libraries_item_layout;

            return new ViewHolder(inflater.inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Library lib = libsList.get(position);
            View view = holder.itemView;
            LibrariesItemLayoutBinding binding = LibrariesItemLayoutBinding.bind(view);
            String transitionName = "license_" + position;
            ViewCompat.setTransitionName(binding.libItem, transitionName);

            String libName = "Unknown";

            if (!lib.getName().trim().isEmpty()) libName = lib.getName();

            binding.libName.setText(libName);
            binding.libVersion.setText(lib.getArtifactVersion());
            String license = "Unknown";

            if (!lib.getLicenses().isEmpty()) {
                license = lib.getLicenses().iterator().next().getName();
                if (license.trim().isEmpty()) license = "Unknown";
            }

            binding.licenseChip.setText(license);

            String owner = "Unknown";

            if (!lib.getDevelopers().isEmpty()) {
                owner = lib.getDevelopers().get(0).getName();
            } else if (lib.getOrganization() != null) {
                owner = lib.getOrganization().getName();
            }

            assert owner != null;
            if (owner.trim().isEmpty()) {
                owner = "Unknown";
            }

            binding.ownerName.setText(owner);

            final String name = libName;
            final String website = lib.getWebsite();
            binding.libItem.setOnClickListener(v -> {
                String licenseContent = "Unknown License";
                if (!lib.getLicenses().isEmpty()) {
                    String content = lib.getLicenses().iterator().next().getLicenseContent();
                    if (content != null) {
                        licenseContent = content;
                    }
                }
                openFragment(binding.libItem, name, licenseContent, website);
            });
        }

        @Override
        public int getItemCount() {
            return libsList.size();
        }

        @Override
        public int getItemViewType(int position) {
            int size = getItemCount();

            if (position == 0) return TYPE_TOP;
            if (position == size - 1) return TYPE_BOTTOM;
            return TYPE_MIDDLE;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }

    private void openFragment(View itemView, String name, String text, String w) {
        if (isClosing) return;
        this.expandedItemView = itemView;
        LibDetailsFragment fragment = LibDetailsFragment.newInstance(name, text, w);

        binding.detailsFragmentContainer.setScaleX(1.0f);
        binding.detailsFragmentContainer.setScaleY(1.0f);
        binding.detailsFragmentContainer.setTranslationX(0f);
        binding.detailsFragmentContainer.setTranslationY(0f);
        binding.detailsFragmentContainer.setClipToOutline(false);
        currentCornerRadius = 0f;

        getChildFragmentManager().beginTransaction()
                .replace(R.id.details_fragment_container, fragment)
                .commitNow();

        int containerColor = MaterialColors.getColor(binding.coordinator, com.google.android.material.R.attr.colorSurfaceContainer);

        MaterialContainerTransform transform = new MaterialContainerTransform(requireContext(), true);
        transform.setStartView(itemView);
        transform.setEndView(binding.detailsFragmentContainer);
        transform.addTarget(binding.detailsFragmentContainer);
        transform.setDrawingViewId(R.id.coordinator);
        transform.setDuration(400L);
        transform.setFadeMode(MaterialContainerTransform.FADE_MODE_CROSS);
        transform.setScrimColor(Color.TRANSPARENT);
        transform.setAllContainerColors(containerColor);

        TransitionManager.beginDelayedTransition(binding.coordinator, transform);
        itemView.setVisibility(View.INVISIBLE);
        binding.detailsFragmentContainer.setVisibility(View.VISIBLE);

        backCallback.setEnabled(true);
    }

    private void closeFragment() {
        if (isClosing || expandedItemView == null) return;
        isClosing = true;
        backCallback.setEnabled(false);

        final View targetItemView = expandedItemView;
        View container = binding.detailsFragmentContainer;
        int rawW = container.getWidth();
        int rawH = container.getHeight();

        if (rawW <= 0 || rawH <= 0) {
            container.setVisibility(View.GONE);
            targetItemView.setVisibility(View.VISIBLE);
            removeDetailsFragment();
            expandedItemView = null;
            isClosing = false;
            return;
        }

        Bitmap snapshot = Bitmap.createBitmap(rawW, rawH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(snapshot);
        container.draw(canvas);

        float pivotX = container.getPivotX();
        float pivotY = container.getPivotY();
        int left = Math.round(container.getLeft() + container.getTranslationX() + pivotX - (pivotX * container.getScaleX()));
        int top = Math.round(container.getTop() + container.getTranslationY() + pivotY - (pivotY * container.getScaleY()));
        int width = Math.max(1, Math.round(rawW * container.getScaleX()));
        int height = Math.max(1, Math.round(rawH * container.getScaleY()));

        ImageView snapshotView = new ImageView(requireContext());
        snapshotView.setImageBitmap(snapshot);
        snapshotView.setScaleType(ImageView.ScaleType.FIT_XY);
        snapshotView.setElevation(container.getElevation());

        CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(width, height);
        lp.leftMargin = left;
        lp.topMargin = top;
        binding.coordinator.addView(snapshotView, lp);
        snapshotView.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        );
        snapshotView.layout(left, top, left + width, top + height);

        float startRadius = currentCornerRadius;
        currentCornerRadius = 0f;
        container.setScaleX(1.0f);
        container.setScaleY(1.0f);
        container.setTranslationX(0f);
        container.setTranslationY(0f);
        container.setClipToOutline(false);
        container.setVisibility(View.GONE);

        int containerColor = MaterialColors.getColor(binding.coordinator, com.google.android.material.R.attr.colorSurfaceContainer);

        MaterialContainerTransform transform = new MaterialContainerTransform(requireContext(), false);
        transform.setStartView(snapshotView);
        transform.setEndView(targetItemView);
        transform.addTarget(targetItemView);
        transform.setDrawingViewId(R.id.coordinator);
        transform.setDuration(350L);
        transform.setFadeMode(MaterialContainerTransform.FADE_MODE_CROSS);
        transform.setScrimColor(Color.TRANSPARENT);
        transform.setAllContainerColors(containerColor);
        transform.setStartShapeAppearanceModel(
                ShapeAppearanceModel.builder()
                        .setAllCornerSizes(startRadius)
                        .build()
        );

        Runnable cleanup = () -> {
            if (!isClosing) return;
            snapshotView.setImageDrawable(null);
            binding.coordinator.removeView(snapshotView);
            removeDetailsFragment();
            expandedItemView = null;
            isClosing = false;
        };

        transform.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                cleanup.run();
            }

            @Override
            public void onTransitionCancel(@NonNull Transition transition) {
                cleanup.run();
            }
        });

        TransitionManager.beginDelayedTransition(binding.coordinator, transform);
        snapshotView.setVisibility(View.INVISIBLE);
        targetItemView.setVisibility(View.VISIBLE);
    }

    private void removeDetailsFragment() {
        Fragment f = getChildFragmentManager().findFragmentById(R.id.details_fragment_container);
        if (f != null && !getChildFragmentManager().isStateSaved()) {
            getChildFragmentManager().beginTransaction().remove(f).commitAllowingStateLoss();
        }
    }

    public class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int bottomSpacing;
        private final int spacing;
        private final int sideSpacing;
        private final int topSpacing;

        public SpacingDecoration(int bottomSpacing) {
            sideSpacing = XUtils.convertToPx(requireContext(), 12f);
            topSpacing = XUtils.convertToPx(requireContext(), 16f);
            this.bottomSpacing = bottomSpacing;
            spacing = XUtils.convertToPx(requireContext(), 2f);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) return;
            if (position == state.getItemCount() - 1) {
                outRect.set(sideSpacing, 0, sideSpacing, bottomSpacing);
            } else if (position == 0) {
                outRect.set(sideSpacing, topSpacing, sideSpacing, spacing);
            } else {
                outRect.set(sideSpacing, 0, sideSpacing, spacing);
            }
        }
    }
}